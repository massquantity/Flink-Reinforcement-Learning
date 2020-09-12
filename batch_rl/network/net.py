import torch
import torch.nn as nn
import torch.nn.functional as F


class ActorMLP(nn.Module):
    def __init__(self, input_dim, action_dim, hidden_size, user_embeds,
                 item_embeds):
        super(ActorMLP, self).__init__()
        self.user_embeds = nn.Embedding.from_pretrained(
            torch.as_tensor(user_embeds), freeze=True,
        )
        self.item_embeds = nn.Embedding.from_pretrained(
            torch.as_tensor(item_embeds), freeze=True,
        )

    #    self.dropout = nn.Dropout(0.5)
        self.fc1 = nn.Linear(input_dim, hidden_size)
        self.fc2 = nn.Linear(hidden_size, hidden_size)
        self.fc3 = nn.Linear(hidden_size, action_dim)

    def get_state(self, data, next_state=False):
        user_repr = self.user_embeds(data["user"])
        item_col = "next_item" if next_state else "item"
        batch_size = data[item_col].size(0)
        item_repr = self.item_embeds(data[item_col]).view(batch_size, -1)
    #    item_repr = torch.mean(self.item_embeds(data[item_col]), dim=1)
        state = torch.cat([user_repr, item_repr], dim=1)
        return state

    def get_action(self, state, tanh=False):
        action = F.relu(self.fc1(state))
        action = F.relu(self.fc2(action))
        action = self.fc3(action)
        if tanh:
            action = F.tanh(action)
        return action

    def forward(self, data, tanh=False):
        state = self.get_state(data)
        action = self.get_action(state, tanh)
        return state, action


class CriticMLP(nn.Module):
    def __init__(self, input_dim, action_dim, hidden_size):
        super(CriticMLP, self).__init__()
        self.fc1 = nn.Linear(input_dim + action_dim, hidden_size)
        self.fc2 = nn.Linear(hidden_size, hidden_size)
        self.fc3 = nn.Linear(hidden_size, 1)

    def forward(self, state, action):
        out = torch.cat([state, action], dim=1)
        out = F.relu(self.fc1(out))
        out = F.relu(self.fc2(out))
        out = self.fc3(out)
        return out.squeeze()


class Beta(nn.Module):
    def __init__(self, input_dim, action_dim, hidden_size):
        super(Beta, self).__init__()
        self.fc1 = nn.Linear(input_dim, hidden_size)
        self.fc2 = nn.Linear(hidden_size, action_dim)

    def forward(self, state):
        out = F.relu(self.fc1(state))
        out = self.fc2(out)
        return out


class VAE(nn.Module):
    def __init__(self, input_dim, action_dim, latent_dim, hidden_size,
                 user_embeds, item_embeds, device=None):
        super(VAE, self).__init__()
        self.user_embeds = nn.Embedding.from_pretrained(
            torch.as_tensor(user_embeds), freeze=True,
        )
        self.item_embeds = nn.Embedding.from_pretrained(
            torch.as_tensor(item_embeds), freeze=True,
        )

        self.encoder1 = nn.Linear(input_dim + action_dim, hidden_size)
        self.encoder2 = nn.Linear(hidden_size, hidden_size)

        self.mean = nn.Linear(hidden_size, latent_dim)
        self.log_std = nn.Linear(hidden_size, latent_dim)

        self.decoder1 = nn.Linear(input_dim + latent_dim, hidden_size)
        self.decoder2 = nn.Linear(hidden_size, hidden_size)
        self.decoder3 = nn.Linear(hidden_size, action_dim)
        self.latent_dim = latent_dim
        self.device = device

    def get_state(self, data, next_state=False):
        user_repr = self.user_embeds(data["user"])
        item_col = "next_item" if next_state else "item"
        batch_size = data[item_col].size(0)
        item_repr = self.item_embeds(data[item_col]).view(batch_size, -1)
    #    item_repr = torch.mean(self.item_embeds(data[item_col]), dim=1)
        state = torch.cat([user_repr, item_repr], dim=1)
        return state

    def forward(self, data, action):
        state = self.get_state(data)
        z = F.relu(self.encoder1(torch.cat([state, action], dim=1)))
        z = F.relu(self.encoder2(z))

        mean = self.mean(z)
        log_std = self.log_std(z).clamp(-4, 15)
        std = log_std.exp()
        z = mean + std * torch.randn_like(std)

        u = self.decode(state, z)
        return state, u, mean, std

    def decode(self, state, z=None):
        if z is None:
            z = torch.randn(state.size(0), self.latent_dim)
            z = z.clamp(-0.5, 0.5).to(self.device)
        action = F.relu(self.decoder1(torch.cat([state, z], dim=1)))
        action = F.relu(self.decoder2(action))
        action = self.decoder3(action)
        return action


class Perturbator(nn.Module):
    def __init__(self, input_dim, action_dim, hidden_size, phi=0.05,
                 action_range=None):
        super(Perturbator, self).__init__()
        self.fc1 = nn.Linear(input_dim + action_dim, hidden_size)
        self.fc2 = nn.Linear(hidden_size, hidden_size)
        self.fc3 = nn.Linear(hidden_size, action_dim)
        self.phi = phi
        self.action_range = action_range

    def forward(self, state, action):
        a = F.relu(self.fc1(torch.cat([state, action], dim=1)))
        a = F.relu(self.fc2(a))
        a = self.fc3(a)
        if self.phi is not None:
            a = self.phi * torch.tanh(a)

        act = action + a
        if self.action_range is not None:
            act = act.clamp(self.action_range[0], self.action_range[1])
        return act

