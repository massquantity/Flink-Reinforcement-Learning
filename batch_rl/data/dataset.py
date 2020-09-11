from torch.utils.data import Dataset, DataLoader
from .session import build_session


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


