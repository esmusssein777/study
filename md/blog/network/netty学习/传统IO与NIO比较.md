# 传统IO与NIO的比较

## 传统的IO特点

阻塞

1. server.accept();
2. inputStream.read(bytes);

这两个地方阻塞

单线程情况下只能有一个客户端，所以适合短连接的情况

线程池多线程可以有多个客户端，但是非常的消耗性能



