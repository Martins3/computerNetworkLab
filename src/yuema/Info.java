/**
 * Created by martin on 17-10-11.
 */
package yuema;
/*
* 1. 用户的信息需不需要实现云端的保存
* 2. 用户的上线提醒
*
* 1. 一个用户创建对象, 如何形成制作对应的类
* 2. 本地是云端的信息不一致, 如何处理的
* 3.
*
*
*
* 1. UDP
*
* 2. 如何检查对方下线, client.close()
*
* 3. 实现 点到点的文件的传输:
*   1. 服务器和 客户端的连接不可以断开
*   2.
*
*
* 4. 如何服务器记得是同一个用户
*
*
* 5. 线程是否需要进行标记, 标记的含义就是的添加相应的, 需要添加的内容是:
*
*
*
* 6. 如何区分到底是普通的内容, 还是账号密码 : 建立连接的前面两行为账号和密码
*
*
* 7. 如何建立p2p
*   1. 查找所有的
*
*
* 8. 如何实现群:
*   1. 创建set, 为所有在set的用户全部发送
*   2.
*
* 9. 如何操控 100线程, 自动化的测试.
*
*
* 10. 服务器端出现问题的时候, 本地如何放置出现问题 ?
* 11. 如何关闭一个线程 ? 似乎是没有必要的搞着一个问题的, 因为 run 结束之后, 然后就是自动结束了
* */

/**
 * 规范:
 * 1. 客户登录的时候同时提供 hostname and listen port
 * 2. 发送消息不需要使用 singleton instance, 接受的消息上传到main,接受的消息全部使用单一通道
 *
 *    server 的接受消息: 相同的上传, 虽然可以本地的处理,但是不处理
 * 3. 所有的传递消息的queue 全部都是 blocking queue
 * 4. 检查所有的socket 的对方主动关闭的情况
 * 5. 所有的发送消息 和 接受消息 全部都是需要使用 sout
 * 6. socket 由于socket 被重用, 所以所有的socket 的关闭都是需要关闭, 并且关闭一次
 * 7. 所有的内部消息的传递使用MessageContent, 在服务器之间传递消息使用 string !
 * 8. 收发消息的时候, 有且只有第一次的时候需要携带说明发送者属性的消息, 以后只有消息本身
 * 9. socket , 所有的thread的局部变量, 线程 和 发送使用的socket 全部都是一一对应的
 * 10. 相同的消息不可以被两个线程检查, 可以使用更加多的blocking queue 来分离
 * 11. 接受的 socket 可以由于对方的关闭而关闭, 但是发送方不会
 * 12. 即使知道不应该发生, 但是使用使用之前, 一定添加 assert
 * 13. common 消息首部一定含有 时间戳子
 * 14. 其他的类都是持有 client 的对象, 如果是之后初始化的, 那么立刻初始化的获取
 *
 *
 *
 *
 *
 * 消息规范:
 *  1. 发送方需要第一次之处自己的 id => MyID hostName listenId , 之后的发送就是只有消息类型 和 文件的类型即可
 *  2. 服务器区分对象使用 connectedUserID, 禁止出现myID, client 也是通过 connectedUserID 进行区分的
 *          也就是上层空间使用connectedUserID, 接受消息的区分使用消息类型的
 *
 *  3. client 的内部, 只有发送到的client的消息才是需要添加 connectType
 *  4. 消息的区分 userInfo 的变量名称 和 MessageContent 的名称conflict
 *  5. common_message 使用myID 表示自己到底来自于哪里 !
 *  6. 关闭线程:
 *      1. 在结束程序的统一的关闭所有的线程
 *      2. 对方下线的时候, 自动关闭接受, 服务器告知关闭发送(stupid)
 *
 *  发送的数据使用:
 *      1. 发送数据使用任务队列的方法
 *
 *  服务器 的收听方应该可以主动的把自己发送程序的线程关闭, 通过收听的异常捕获!
 *
 *  1. 服务器需要处理 下线用户需要删除
 *  2.
 *
 * bug log:
 *  1. 一个文件发送完毕之后, 没有办法发送第二个文件
 *  2. 为什么发送的ip 地址是本机器的地址, 然后的问题, 好像的Ip的格式不是很对, 但是依旧可以通信, 是不是有的地方数值写死了导致的
 *
 * todo:
 *1. 客户端的停止发送:
 *  可以来只会来自于一个地方, 被服务器告知该人下线了
 *2. 客户显示的消息总是排好序的
 *3. 地址采用相对的地址才是可以的
 * 4. 服务器 挂机之后, 客户端无法发送消息
 * 5 服务器可以控制客户下线
 * 6. 防止相同的人登录两次, 放置自己添加自己为好友
 * 7. 连续的消息 服务器发送含有bug, 服务器检测原理 线程TreeSet 是否持有该对象, 该对象还没有添加到 TreeSet 中间的时候, 就开始添加第二条消息,
 * 这一个时候会创建出来两个sender
 * 8. 实现群的功能
 * 9. 保证最后的一个 ack 可以到达
 * 10. 取消发送文件
 * 11. 跨平台 文件名称读取 文件的位置的获取
 * 12. 当前的文件传送的模式会导致缺少有人出现空等待的状态, 只有当前可以发送的时候这一个时候才可以
 * 发送传输文件请求
 * 13. 动态调节windowSize
 * 14. 动态添加时间
 * 15. 添加拒绝接受的部分的
 * 16. 特殊的处理最后的一个包写入到 byteInRam 的放置越界的问题 !
 * 17. 对于offline 的文件也是可以传送文件的
 * 18. 如果对面突然结束传输文件的时候, 接受文件的用户应该可以主动打断当前的文件的传输
 * 19. 没有添加测速功能, 也没有的实现测试丢包, 没有实现可变的大小的文件的窗口
 * 20. 服务器挂机, 客户端应该有应有的处理
 * 21. 注册的问题似乎可以完善一下
 * 22. 没有处理两个问题, 如果有人登录失败之后服务器,人然后使用其他的用户名称进行登录, 如何处理吧
 * 23. 实在是不知道应该如何使用 解决的办法为 心跳包 和 第一个
 *
 *
 *
 *
 *
 *
 *
 *
 * */