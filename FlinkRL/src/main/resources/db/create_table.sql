CREATE TABLE IF NOT EXISTS record
(
    user int NOT NULL,
    item int  NOT NULL,
    time  TIMESTAMP NOT NULL,
    PRIMARY KEY (user, item)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS top
(
    item  int  NOT NULL,
    rank  VARCHAR(5)  NOT NULL,
    PRIMARY KEY (item, rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



