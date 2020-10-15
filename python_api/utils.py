import numpy as np
import torch
from net import Actor, PolicyPi, VAE, Perturbator


def load_model_reinforce(
    model_path, 
    user_embeddings_path, 
    item_embeddings_path, 
    input_dim, 
    action_dim, 
    hidden_size, 
    device
):
    with open(user_embeddings_path, "rb") as f:
        user_embeddings = np.load(f)
    with open(item_embeddings_path, "rb") as f:
        item_embeddings = np.load(f)
    model = PolicyPi(
        input_dim, action_dim, hidden_size, user_embeddings, item_embeddings
    )
    model.load_state_dict(torch.load(model_path, map_location=device))
    model.eval()
    return model


def load_model_ddpg(
    model_path, 
    user_embeddings_path, 
    item_embeddings_path, 
    input_dim, 
    action_dim, 
    hidden_size, 
    device
):
    with open(user_embeddings_path, "rb") as f:
        user_embeddings = np.load(f)
    with open(item_embeddings_path, "rb") as f:
        item_embeddings = np.load(f)
    model = Actor(
        input_dim, action_dim, hidden_size, user_embeddings, item_embeddings
    )
    model.load_state_dict(torch.load(model_path, map_location=device))
    model.eval()
    return model


def load_model_bcq(
    model_path, 
    user_embeddings_path, 
    item_embeddings_path, 
    input_dim, 
    action_dim, 
    hidden_size, 
    device
):
    with open(user_embeddings_path, "rb") as f:
        user_embeddings = np.load(f)
    with open(item_embeddings_path, "rb") as f:
        item_embeddings = np.load(f)
    generator = VAE(
        input_dim, action_dim, action_dim * 2, hidden_size, 
        user_embeddings, item_embeddings
    )
    perturbator = Perturbator(input_dim, action_dim, hidden_size)
    checkpoint = torch.load(model_path, map_location=device)
    generator.load_state_dict(checkpoint["generator"])
    perturbator.load_state_dict(checkpoint["perturbator"])
    generator.eval()
    perturbator.eval()
    return generator, perturbator
