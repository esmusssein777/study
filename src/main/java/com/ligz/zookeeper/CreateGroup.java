package com.ligz.zookeeper;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * 我们使用zookeeper的Java API来创建一个/zoo的组节点
 * author:ligz
 */
public class CreateGroup implements Watcher {
    private static final int SESSION_TIMEOUT = 5000;
    private ZooKeeper zk;
    private CountDownLatch connectedSignal = new CountDownLatch(1);

    /**
     * 在连接函数中创建了zookeeper的实例，然后建立与服务器的连接。建立连接函数会立即返回，所以我们需要等待连接建立成功后再进行其他的操作。
     * 我们使用CountDownLatch来阻塞当前线程，直到zookeeper准备就绪。这时，我们就看到Watcher的作用
     * @param hosts
     */
    public void connect(String hosts) throws IOException, InterruptedException {
        zk = new ZooKeeper(hosts, SESSION_TIMEOUT, this);
        connectedSignal.await();
    }

    @Override
    public void process(WatchedEvent event) { // Watcher interface
        if (event.getState() == Event.KeeperState.SyncConnected) {
            connectedSignal.countDown();
        }
    }

    public void create(String groupName) throws KeeperException,
            InterruptedException {
        String path = "/" + groupName;
        //一是znode的path；二是znode的内容（一个二进制数组），三是一个access control list(ACL，访问控制列表) 最后是znode的性质
        String createdPath = zk.create(path, null/*data*/, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);
        System.out.println("Created " + createdPath);
    }

    public void close() throws InterruptedException {
        zk.close();
    }

    public static void main(String[] args) throws Exception {
        CreateGroup createGroup = new CreateGroup();
        createGroup.connect(args[0]);
        createGroup.create(args[1]);
        createGroup.close();
    }
}
