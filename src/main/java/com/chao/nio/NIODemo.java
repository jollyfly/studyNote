package com.chao.nio;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by wanzhichao on 2017/6/8.
 */
public class NIODemo {


    public static void main(String[] args) throws Exception{
        ByteBuffer send = ByteBuffer.allocate(1024);
        ByteBuffer receive = ByteBuffer.allocate(1024);
        /*
         * FileChannel
//         */
//        try (FileInputStream fis = new FileInputStream(NIODemo.class.getClassLoader().getResource("test.txt").getFile());
//             //通过FileInputStream对象获取FileChannel
//             FileChannel fic = fis.getChannel();) {
//            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
//            int bytesRead = 0;
//            //使用通道将数据读取到Buffer对象
//            while ((bytesRead = fic.read(byteBuffer)) != -1) {
//                String content = new String(byteBuffer.array(), 0, bytesRead);
//                System.out.println(content);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(6666));
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            send.put("我爱北京天安门".getBytes());
            while (true) {
                int count = selector.select();
                System.out.println("count=" + count);
                // 返回此选择器的已选择键集。
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    // 这里记得手动的把他remove掉，不然selector中的selectedKeys集合不会自动去除
                    iterator.remove();
                    ServerSocketChannel server = null;
                    SocketChannel client = null;
                    String receiveText;
                    String sendText;
                    int counts = 0;

                    // 测试此键的通道是否已准备好接受新的套接字连接。
                    if (selectionKey.isAcceptable()) {
                        System.out.println("selectionKey.isAcceptable()");
                        // 返回为之创建此键的通道。
                        server = (ServerSocketChannel) selectionKey.channel();

                        // 此方法返回的套接字通道（如果有）将处于阻塞模式。
                        client = server.accept();
                        // 配置为非阻塞
                        client.configureBlocking(false);
                        // 注册到selector，等待连接
                        client.register(selector, SelectionKey.OP_READ
                                | SelectionKey.OP_WRITE);
                    }
                    if (selectionKey.isReadable()) {
                        System.out.println("selectionKey.isReadable()");
                        // 返回为之创建此键的通道。
                        client = (SocketChannel) selectionKey.channel();
                        // 将缓冲区清空以备下次读取
                        receive.clear();
                        // 读取服务器发送来的数据到缓冲区中
                        client.read(receive);

//                System.out.println(new String(receive.array()));

                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                    } else if (selectionKey.isWritable()) {
                        System.out.println("selectionKey.isWritable()");
                        // 将缓冲区清空以备下次写入
                        send.flip();
                        // 返回为之创建此键的通道。
                        client = (SocketChannel) selectionKey.channel();

                        // 输出到通道
                        client.write(send);

//                    selectionKey.interestOps(SelectionKey.OP_READ);
                    }
                }
            }
        } catch (Exception e) {

        }
        Thread clientRunner = new Thread(() -> {

        });

    }

}
