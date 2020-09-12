import torch
import torch.nn as nn
from torch.distributions import Categorical


class Reinforce(nn.Module):
    def __init__(self, actor, actor_optim, beta, beta_optim, gamma=0.99,
                 k=10, weight_clip=2.0, offpolicy_correction=True, topk=True,
                 device=torch.device("cpu")):
        super(Reinforce, self).__init__()
        self.actor = actor
        self.actor_optim = actor_optim
        self.beta = beta
        self.beta_optim = beta_optim
        self.beta_criterion = nn.CrossEntropyLoss()
        self.gamma = gamma
        self.k = k
        self.weight_clip = weight_clip
        self.offpolicy_correction = offpolicy_correction
        self.topk = topk
        self.device = device

    def update(self, data):
        state, actor_logits = self.actor(data)
        actor_dist = Categorical(logits=actor_logits)
        actor_logp = actor_dist.log_prob(data["action"])

        beta_logits = self.beta(state.detach())
        beta_dist = Categorical(logits=beta_logits)
        beta_logp = beta_dist.log_prob(data["action"]).detach()

        importance_weight = self._compute_weight(actor_logp, beta_logp)
        lambda_k = self._compute_lambda_k(actor_logp)

        policy_loss = -(importance_weight * lambda_k * data["return"]
                        * actor_logp).mean()
        beta_loss = self.beta_criterion(beta_logits, data["action"])

        self.actor_optim.zero_grad()
        policy_loss.backward()
        self.actor_optim.step()

        self.beta_optim.zero_grad()
        beta_loss.backward()
        self.beta_optim.step()

        info = {'policy_loss': policy_loss.detach().item(),
                'beta_loss': beta_loss.detach().item(),
                'importance_weight': importance_weight.mean().item(),
                'lambda_k': lambda_k.mean().item(),
                'actor_dist': actor_dist}
        return info

    def _compute_weight(self, actor_logp, beta_logp):
        if self.offpolicy_correction:
            importance_weight = torch.exp(actor_logp - beta_logp).detach()
            wc = torch.tensor([self.weight_clip]).to(self.device)
            importance_weight = torch.min(importance_weight, wc)
        else:
            importance_weight = torch.tensor([1.]).float().to(self.device)
        return importance_weight

    def _compute_lambda_k(self, actor_logp):
        return self.k * (
            (1. - actor_logp.exp()).pow(self.k - 1)
        ).detach() if self.topk else torch.tensor([1.]).float().to(self.device)

    def compute_loss(self, data):
        state, actor_logits = self.actor(data)
        actor_dist = Categorical(logits=actor_logits)
        actor_logp = actor_dist.log_prob(data["action"])

        beta_logits = self.beta(state.detach())
        beta_dist = Categorical(logits=beta_logits)
        beta_logp = beta_dist.log_prob(data["action"]).detach()

        importance_weight = self._compute_weight(actor_logp, beta_logp)
        lambda_k = self._compute_lambda_k(actor_logp)

        policy_loss = -(importance_weight * lambda_k * data["return"]
                        * actor_logp).mean()
        beta_loss = self.beta_criterion(beta_logits, data["action"])

        info = {'policy_loss': policy_loss.detach().item(),
                'beta_loss': beta_loss.detach().item(),
                'importance_weight': importance_weight.mean().item(),
                'lambda_k': lambda_k.mean().item(),
                'actor_dist': actor_dist}
        return info

    def get_distribution(self, data):
        with torch.no_grad():
            _, actor_logits = self.actor(data)
            actor_dist = Categorical(logits=actor_logits)
        return actor_dist

    def forward(self, state):
        actor_logits = self.actor.get_action(state)
        actor_dist = Categorical(logits=actor_logits)
        _, rec_idxs = torch.topk(actor_dist.probs, 10, dim=1)
        return rec_idxs

