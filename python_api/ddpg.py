import numpy as np
import torch
from typing import Optional, List
from fastapi import FastAPI, HTTPException, Body
from pydantic import BaseModel, Field
from utils import load_model_ddpg

app = FastAPI()
hist_num = 10
action_dim = 32
input_dim = action_dim * (hist_num + 1)
hidden_size = 64
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model_path = "model_ddpg.pt"
user_embeddings_path = "tianchi_user_embeddings.npy"
item_embeddings_path = "tianchi_item_embeddings.npy"

model = load_model_ddpg(
    model_path, user_embeddings_path, item_embeddings_path, 
    input_dim, action_dim, hidden_size, device=device
)


class Seq(BaseModel):
    user: List[int]
    item: List[List[int]] = Field(..., example=[[1,2,3,4,5,6,7,8,9,10]])
    n_rec: int


class State(BaseModel):
    user: List[int]
    embedding: List[List[float]]
    n_rec: int


@app.post("/{algo}")
async def recommend(algo: str, seq: Seq) -> list:
    if algo == "ddpg":
        with torch.no_grad():
            data = {
                "user": torch.as_tensor(seq.user), 
                "item": torch.as_tensor(seq.item)
            }
            _, action = model(data)
            scores = torch.matmul(action, model.item_embeds.weight.T)
            _, res = torch.topk(scores, seq.n_rec, dim=1, sorted=False)
        # return f"Recommend {seq.n_rec} items for user {seq.user}: {res}"
        return res.tolist()
    else:
        raise HTTPException(status_code=404, detail="wrong algorithm.")


@app.post("/{algo}/state")
async def recommend_with_state(algo: str, state: State):
    if algo == "ddpg":
        with torch.no_grad():
            data = torch.as_tensor(state.embedding)
            action = model.get_action(data)
            scores = torch.matmul(action, model.item_embeds.weight.T)
            _, res = torch.topk(scores, state.n_rec, dim=1, sorted=False)
            # return f"Recommend {state.n_rec} items for user {state.user}: {res}"
        return res.tolist()
    else:
        raise HTTPException(status_code=404, detail="wrong algorithm.")


# gunicorn ddpg:app -w 4 -k uvicorn.workers.UvicornWorker
# curl -X POST "http://127.0.0.1:8000/ddpg" -H "accept: application/json" -d '{"user": [1], "item": [[1,2,3,4,5,6,7,8,9,10]], "n_rec": 8}'
