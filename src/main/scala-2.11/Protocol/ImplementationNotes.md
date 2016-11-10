2016年11月10日 星期四

在测试 PaxosSpec 时发现了异常, Replica1 和 Replica2 的结果不一致, 也就是俩人最终 decision
序列发生了冲突

```
Map(1 -> Command(client1,reqId1,TestOperation)) did not contain element (1,Command(client2,reqId2,TestOperation))
```

这个测试用例是

```scala
        tempLeader1 ! ProposalMessage(1, cmd1)
        tempLeader2 ! ProposalMessage(1, cmd2)

        Thread.sleep(1000) // wait a little longer

        val replicaState1 = Replica.getInternalState(replica1)
        val replicaState2 = Replica.getInternalState(replica2)
        
        replicaState1.decision.get(1) match {
          case Some(cmd1) =>
            println("cmd1 wins")
            replicaState2.decision should contain (1 -> cmd1)
          case Some(cmd2) =>
            println("cmd2 wins")
            replicaState2.decision should contain (1 -> cmd2)
        }
```

消息是直接发给 leader 的, 没有经过 replica, 出现 replica 不一致的问题, 我想应该是 Leader 变成 Active 时没有学到该学
的知识, 也就是说, Leader 的实现有问题。

不过想验证自己的想法是很困难的, 毕竟这个不能固定重现, 只能通过打 log 的方式查看数据的流动。我发现两个问题, 第一个是
cmd1 总是 win, 第二个是出错的概率蛮大, 有 30% 左右的可能性。

bug 解决了, 是因为把应该返回 maxBallotNumber 的地方返回了 ballotNumber, 导致 leader 没有抢占 view

### Final test

写了这么多 test, 最后一个 HeavyTest 总结, 发出 100 个请求, 然后查看 replica state machine 的结果是否一致。

最终的结果往往是要比 100 大的, 在什么场景下会大呢?
