from collections import defaultdict
import numpy as np
import pandas as pd
from torch.utils.data import Dataset, DataLoader
from .session import build_sess_end, build_session
from .split import split_by_ratio


class RLDataset(Dataset):
    def __init__(self, data):
        self.data = data

    def __getitem__(self, index):
        user = self.data["user"][index]
        items = self.data["item"][index]
        res = {"user": user,
               "item": items[:-1],
               "action": items[-1],
               "reward": self.data["reward"][index],
               "done": self.data["done"][index],
               "next_item": items[1:]}
        return res

    def __len__(self):
        return len(self.data["item"])


class EvalRLDataset(Dataset):
    def __init__(self, data):
        self.data = data

    def __getitem__(self, index):
        user = self.data["user"][index]
        items = self.data["item"][index]
        res = {"user": user,
               "item": items[:-1],
               "action": items[-1],
               "reward": self.data["reward"][index],
               "done": self.data["done"][index],
               "next_item": items[1:]}
        return res

    def __len__(self):
        return len(self.data["item"])


def build_dataloader(n_users, hist_num, train_user_consumed, test_user_consumed,
                     batch_size, sess_mode="one", train_sess_end=None,
                     test_sess_end=None, n_workers=0):
    """Construct DataLoader for pytorch model.

    Parameters
    ----------
    n_users : int
        Number of users.
    hist_num : int
        A fixed number of history items that a user interacted. If a user has
        interacted with more than `hist_num` items, the front items will be
        truncated.
    train_user_consumed : dict
        Items interacted by each user in train data.
    test_user_consumed : dict
        Items interacted by each user in test data.
    batch_size : int
        How many samples per batch to load.
    sess_mode : str
        Ways of representing a session.
    train_sess_end : dict
        Session end mark for each user in train data.
    test_sess_end : dict
        Session end mark for each user in test data.
    n_workers : int
        How many subprocesses to use for data loading.

    Returns
    -------
    train_rl_loader : DataLoader
        Train dataloader for training.
    test_rl_loader : DataLoader
        Test dataloader for testing.
    """

    train_session = build_session(n_users, hist_num, train_user_consumed,
                                  train=True, sess_end=train_sess_end,
                                  sess_mode=sess_mode)
    test_session = build_session(n_users, hist_num, train_user_consumed,
                                 test_user_consumed, train=False,
                                 sess_end=test_sess_end, sess_mode=sess_mode)

    train_rl_data = RLDataset(train_session)
    test_rl_data = EvalRLDataset(test_session)

    train_rl_loader = DataLoader(train_rl_data, batch_size=batch_size,
                                 shuffle=True, num_workers=n_workers)
    test_rl_loader = DataLoader(test_rl_data, batch_size=batch_size*2,
                                shuffle=False, num_workers=n_workers)
    return train_rl_loader, test_rl_loader


def process_data(path, columns=None, test_size=0.2, time_col="time",
                 sess_mode="one", interval=None):
    """Split and process data before building dataloader.

    Parameters
    ----------
    path : str
        File path.
    columns : list, optional
        Column names for the original data.
    test_size : float
        Test data size to split from original data.
    time_col : str
        Specify which column represents time.
    sess_mode : str
        Ways of representing a session.
    interval : int
        Interval between different sessions.

    Returns
    -------
    n_users : int
        Number of users.
    n_items : int
        Number of items.
    train_user_consumed : dict
        Items interacted by each user in train data.
    test_user_consumed : dict
        Items interacted by each user in test data.
    train_sess_end : dict
        Session end mark for each user in train data.
    test_sess_end : dict
        Session end mark for each user in test data.
    """

    column_names = columns if columns is not None else None
    data = pd.read_csv(path, sep=",", names=column_names)
    assert time_col in data.columns, "must specify correct time column name..."

    data = data.sort_values(by=time_col).reset_index(drop=True)
    data.label = 1.
    train_data, test_data = split_by_ratio(data, shuffle=False,
                                           test_size=test_size,
                                           pad_unknown=True,
                                           seed=42)

    train_data, test_data = map_unique_value(train_data, test_data)
    n_users = train_data.user.nunique()
    n_items = train_data.item.nunique()
    # groupby too slow...
    # train_user_consumed = train_data.groupby("user")["item"].apply(
    #    lambda x: list(x)).to_dict()
    # test_user_consumed = test_data.groupby("user")["item"].apply(
    #    lambda x: list(x)).to_dict()
    train_user_consumed = _build_interaction(train_data)
    test_user_consumed = _build_interaction(test_data)

    train_sess_end = build_sess_end(train_data, sess_mode, time_col, interval)
    test_sess_end = build_sess_end(test_data, sess_mode, time_col, interval)
    return (n_users, n_items, train_user_consumed, test_user_consumed,
            train_sess_end, test_sess_end)


def map_unique_value(train_data, test_data):
    for col in ["user", "item"]:
        unique_vals = np.unique(train_data[col])
        mapping = dict(zip(unique_vals, range(len(unique_vals))))
        train_data[col] = train_data[col].map(mapping)
        test_data[col] = test_data[col].map(mapping)
        if test_data[col].isnull().any():
            col_type = train_data[col].dtype
            test_data[col].fillna(len(unique_vals), inplace=True)
            test_data[col] = test_data[col].astype(col_type)
    return train_data, test_data


def _build_interaction(data):
    res = defaultdict(list)
    for u, i in zip(data.user, data.item):
        res[u].append(i)
    return res


def _build_feat_map(data, n_items, static_feat=None, dynamic_feat=None):
    feat_map = dict()
    if static_feat is not None:
        for feat in static_feat:
            feat_map[feat] = dict(zip(data["user"], data[feat]))
            feat_map[feat + "_vocab"] = data[feat].nunique()
    if dynamic_feat is not None:
        for feat in dynamic_feat:
            feat_map[feat] = dict(zip(data["item"], data[feat]))
            feat_map[feat + "_vocab"] = data[feat].nunique()
            # avoid oov item features
            feat_map[feat][n_items] = feat_map[feat + "_vocab"]
    return feat_map


