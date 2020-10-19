# Flink-Reinforcement-Learning

### `English`  &nbsp;  `简体中文`

<br>

FlinkRL 是一个基于 Flink 和强化学习的推荐系统。具体来说，Flink 因其高性能有状态流处理而闻名，这能使系统快速而准确地响应用户请求。而强化学习则擅长规划长期收益，以及根据用户实时反馈快速调整推荐结果。二者的结合使得推荐系统能够捕获用户兴趣的动态变化规律并提供富有洞察力的推荐结果。

FlinkRL 主要用于在线推理，而离线训练在另一个仓库 [DBRL](https://github.com/massquantity/DBRL) 中实现，下图是整体系统架构：

![](/home/massquantity/Documents/Flink_RL/readme1.png)



## 主要流程

为模拟在线环境，由一个数据集作为生产者发送数据到 Kafka，Flink 则从 Kafka 消费数据。之后，Flink 可以执行三种任务：

+ 保存用户行为日志到 MongoDB 和 MySQL，日志可用于离线模型训练。
+ 计算实时 top-N 热门物品，保存到 Redis 。
+ 收集用户近期消费过的物品，发送数据到由 [FastAPI](https://github.com/tiangolo/fastapi) 创建的网络服务中，得到推荐结果并将其存到 MongoDB 。

之后在线服务器可以直接从数据库中调用推荐结果和热门物品发送到客户端。这里使用 FastAPI 建立另一个服务是因为线下训练使用的是 PyTorch，对于 PyTorch 模型现在貌似没有一个统一的部署方案，所以 FastAPI 用于载入模型，预处理和产生最终推荐结果。



## 数据

数据来源于天池的一个比赛，详情可参阅[官方网站](https://tianchi.aliyun.com/competition/entrance/231721/information?lang=zh-cn) ，注意这里只是用了第二轮的数据。也可以从 [Google Drive](https://drive.google.com/file/d/1erBjYEOa7IuOIGpI8pGPn1WNBAC4Rv0-/view?usp=sharing) 下载。



## 使用步骤

Python 依赖库：python>=3.6, numpy, pandas, torch>=1.3, tqdm, FastAPI

首先 clone 两个仓库

```shell
$ git clone https://github.com/massquantity/Flink-Reinforcement-Learning.git
$ git clone https://github.com/massquantity/DBRL.git
```

首先使用 `DBRL` 作离线训练。下载完数据后，解压并放到 `DBRL/dbrl/resources` 文件夹中。原始数据有三张表：`user.csv`, `item.csv`, `user_behavior.csv` 。首先用脚本 `run_prepare_data.py` 过滤掉一些行为太少的用户并将所有特征合并到一张表。接着用 `run_pretrain_embeddings.py` 为每个用户和物品预训练 embedding：

```shell
$ cd DBRL/dbrl
$ python run_prepare_data.py
$ python run_pretrain_embeddings.py --lr 0.001 --n_epochs 4
```

可以调整一些参数如 `lr` 和 `n_epochs`  来获得更好的评估效果。接下来开始训练模型，现在在 `DBRL` 中有三种模型，任选一种即可：

```shell
$ python run_reinforce.py --n_epochs 5 --lr 1e-5
$ python run_ddpg.py --n_epochs 5 --lr 1e-5
$ python run_bcq.py --n_epochs 5 --lr 1e-5
```

到这个阶段，`DBRL/resources` 中应该至少有 6 个文件：

+ `model_xxx.pt`, 训练好的 PyTorch 模型。
+ `tianchi.csv`, 转换过的数据集。
+ `tianchi_user_embeddings.npy`,  `npy` 格式的 user 预训练 embedding。
+ `tianchi_item_embeddings.npy`,  `npy` 格式的 item 预训练 embedding。
+ `user_map.json`,  将原始用户 id 映射到模型中 id 的 json 文件。
+ `item_map.json`,  将原始物品 id 映射到模型中 id 的 json 文件。



离线训练完成后，接下来使用 `FlinkRL` 作在线推理。首先将三个文件 `model_xxx.pt`, `tianchi_user_embeddings.npy`, `tianchi_item_embeddings.npy` 放入 `Flink-Reinforcement-Learning/python_api` 文件夹。确保已经安装了[FastAPI](https://github.com/tiangolo/fastapi), 就可以启动服务了：

```shell
$ gunicorn reinforce:app -w 4 -k uvicorn.workers.UvicornWorker   # if the model is reinforce
$ gunicorn ddpg:app -w 4 -k uvicorn.workers.UvicornWorker   # if the model is ddpg
$ gunicorn bcq:app -w 4 -k uvicorn.workers.UvicornWorker   # if the model is bcq
```



另外三个文件：`tianchi.csv`, `user_map.json`, `item_map.json` 在 Flink 中使用，原则上可以放在任何地方，只要在 `Flink-Reinforcement-Learning/FlinkRL/src/main/resources/config.properties` 中指定绝对路径。

如果想要快速开始，可以直接将 `FlinkRL` 导入到 `IntelliJ IDEA` 这样的 IDE 中。而要在集群上运行，则使用 Maven 达成 jar 包：

```shell
$ cd FlinkRL
$ mvn clean package
```

将生成的 `FlinkRL-1.0-SNAPSHOT-jar-with-dependencies.jar` 放到 Flink 安装目录。现在假设 `Kafka`, `MongoDB` and `Redis`  都已然启动，然后就可以启动 Flink 集群并执行任务：

```shell
$ kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic flink-rl  # cereate a topic called flink-rl in Kafka
$ use flink-rl  # cereate a database called flink-rl in MongoDB
```

```shell
$ # first cd into the Flink installation folder
$ ./bin/start-cluster.sh  # start the cluster
$ ./bin/flink run --class com.mass.task.FileToKafka FlinkRL-1.0-SNAPSHOT-jar-with-dependencies.jar   # import data into Kafka
$ ./bin/flink run --class com.mass.task.RecordToMongoDB FlinkRL-1.0-SNAPSHOT-jar-with-dependencies.jar  # save log to MongoDB
$ ./bin/flink run --class com.mass.task.IntervalPopularItems FlinkRL-1.0-SNAPSHOT-jar-with-dependencies.jar  # compute realtime popular items
$ ./bin/flink run --class com.mass.task.SeqRecommend FlinkRL-1.0-SNAPSHOT-jar-with-dependencies.jar   # recommend items using reinforcement learning model
$ ./bin/stop-cluster.sh  # stop the cluster
```

打开 [http://localhost:8081](http://localhost:8081/) 可使用 Flink WebUI :

![](/home/massquantity/Documents/Flink_RL/5.png)



FastAPI 中也提供了交互式的 WebUI，可访问 http://127.0.0.1:8000/docs :

![](/home/massquantity/Documents/Flink_RL/6.jpg)










