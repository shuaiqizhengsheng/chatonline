package day06;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务端应用程序
 * 
 * @author zhengsheng
 *
 */
public class Server {
	private ServerSocket server;
    //线程池，用于管理客户端链接的交互线程
	private ExecutorService threadPool;
	//保存所有客户端输出流的集合
	private List<PrintWriter> allOut;
	/*
	 * 构造方法，用于初始化服务器端
	 */
	public Server() throws IOException {

		try {
			/*
			 * 创建ServerSocket时需要指定服务端口
			 */
			System.out.println("初始化服务端");
			server = new ServerSocket(8088);
			//初始化线程池
		    threadPool=Executors.newFixedThreadPool(50);
			
		    //初始化存放所有客户端输出流的集合
		    allOut = new ArrayList<PrintWriter>();
		    
			System.out.println("服务端初始化完毕");
		} catch (IOException e) {

			e.printStackTrace();
			throw e;
		}

	}

	/*
	 * 服务端开始工作的方法
	 */
	public void start() {
		try {
			/*
			 * ServerSocket的accept方法 用于监听8088端口，等待客户端的连接 该方法是一个阻塞的方法，知道一个
			 * 哭护短链接，否则该方法一直阻塞 若一个客户端连接了，会返回客户端的 Socket
			 */
			while(true){
			System.out.println("等待客户端连接");
			Socket socket = server.accept();
			/*
			 * 当一个客户端连接后，启动一个线程ClientHandler
			 * 将该客户端的Socket传入，使得该线程与该客户端的交互。
			 * 这样，我们能再次进入下你还，接收下一个客户端的连接了
			 */
			Runnable handler = new ClientHandler(socket);
//			Thread t = new Thread(handler);
	//		t.start();
			/*
			 * 使用线程池分配空闲线程来处理当前连接的客户端
			 */
			threadPool.execute(handler);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*
	 * 将给定的输出流存入共享集合
	 */
	public synchronized void addOut(PrintWriter pw){
		allOut.add(pw);
	}
	/*
	 * 将给定的输出流从共享集合中删除
	 */
	public synchronized void removeOut(PrintWriter pw){
		allOut.remove(pw);
	}
	/*
	 * 将给定的消息转发给所有客户端
	 */
	public synchronized void sendMessage(String message){
		for(PrintWriter pw : allOut){
			pw.println(message);
		}
	}

	public static void main(String[] args) {
		Server server;
		try {
			server = new Server();
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("服务端初始化失败");
		}

	}

	/*
	 * 服务端中的一个县城，用于与某个客户端交互 使用线程的目的是使得服务端可以处理多客户端
	 */
	class ClientHandler implements Runnable {
		// 当前线程处理的客户端的Socket
		private Socket socket;
		//当前客户端的IP
		private String ip;
		//当前客户端的昵称
		private String nickName;
		/*
		 * 根据给定的客户端的Socket，创建线程体 （非 Javadoc）
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public ClientHandler(Socket socket) {
			this.socket = socket;
			
			/*
			 * 通过socket获取远端的地址信息 对于服务端而言，远端就是客户端了
			 */
			InetAddress address = socket.getInetAddress();
			/**
			 * 获取远端计算机IP地址
			 */
			ip = address.getHostAddress();
			// address.getCanonicalHostName();
			int port = socket.getPort();
			System.out.println(ip + "：" + port + "客户端连接成功");
			//改为昵称通知
//			sendMessage("["+ip+"]上线了");
		}

		/*
		 * 该县城会将当前Socket中的输入流获取用来循环读取 客户端发送的消息
		 * 
		 */
		public void run() {
			PrintWriter pw=null;//定义在try语句外的目的是，为了在finally中可以引用到
			try {
				/*
				 * 为了向服务端与客户端交互发送信息，我们需要通过socket获取输出流
				 */
				OutputStream out = socket.getOutputStream();
				//转化为字符流，用于指定编码集
				OutputStreamWriter osw = new OutputStreamWriter(out, "utf-8");
				//创建缓冲字符输出流
				pw = new PrintWriter(osw,true);
				
				/*
				 * 将该客户端的输出流存入共享集合以便使得该客户端也能接受服务端转发
				 * 的消息
				 */
//				allOut.add(pw);
				addOut(pw);
				//输出当前在线人数
				System.out.println("当前在线人数为："+allOut.size());
				
				
				/**
				 * 通过刚刚连上的客户端的Socke获取 输入流，来读取客户端发送过来的信息
				 */

				InputStream in = socket.getInputStream();
				/*
				 * 将字节输入流包装为字符输入流，这样可以指定编码集来读取每一个字符
				 */
				InputStreamReader isr = new InputStreamReader(in, "UTF-8");
				/*
				 * 将字符输入流转化为缓冲字符输入流 这样就可以以行为单位读取字符串了
				 */
				BufferedReader br = new BufferedReader(isr);
				/*
				 * 当创建好当前客户端的输入流后读取的第一个字符串，应当是昵称
				 */
				nickName = br.readLine();
				//通知所有客户端，当前用户上线了
				sendMessage("["+nickName+"]上线了");
				
				String message = null;
				// 读取0客户端发送过来的一行字符串
				/*
				 * 读取客户端发送过来的信息 windows与linux存在一定的差异：
				 * linux：当客户端与无逾期断开连接后我们通过输入流会读取到null
				 * 但这是合乎逻辑的，因为缓冲流的readLine()若返回null就无法通过
				 * 该流再读取到信息。参考之前的服务器文本文件判断。 windows：当客户端与服务器断开连接后
				 * readLine()方法会抛出异常
				 */

				while ((message = br.readLine()) != null) {
//					System.out.println("客户端说：" + message);
//					pw.println(message);
					/*
					 * 当读取到客户端发送过来的一条消息后，将该消息转发给所有客户端
					 * 
					 */
//				     for (PrintWriter o : allOut){
//				    	 o.println(message);
//				     }
					sendMessage(nickName+"说："+message);
				}

			} catch (Exception e) {
				// 在Windows中的客户端，报错同窗是因为客户端断开了链接
			} finally {
				/*
				 * 首先将该客户端的输出流从共享集合中删除
				 */
//				allOut.remove(pw);
				removeOut(pw);
				//输出当前在线人数
				System.out.println("当前在线人数为："+allOut.size());
				//通知其他用户，该用户下线了
				sendMessage("["+nickName+"]下线了");
				
				try {
					socket.close();
				} catch (IOException e) {

				}
				System.out.println("一个客户端下线了...");
			}
		}
	}
}
