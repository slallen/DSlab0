import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.yaml.snakeyaml.Yaml;

public class MessagePasser {
	private String configuration_file;
	private String local_name;
	private static ArrayList<Rule> send_rules;
	private static ArrayList<Rule> recv_rules;
	private static Queue<Message> send_queue = new LinkedList<Message>();
	private HashMap<String, Socket> connections; // stores <dest_name, socket>
	private HashMap<String, Host> hosts = new HashMap<String,Host>();// stores <dest_name, host>
	private int server_port = 12345; // this value is randomly choosed
	
	public MessagePasser(String configuration_filename, String local_name) throws IOException{
		this.configuration_file = configuration_filename;
		this.local_name = local_name;
		send_rules = new ArrayList<Rule>();
		recv_rules = new ArrayList<Rule>();
		
		/*TODO use yaml to load configuration file, updates rules*/		
		if ( !parse_configuration(this.configuration_file) ){
			System.out.println("cannot init configuration exit");
			return;
		}
		
		/* start one thread to listen */
		ServerSocket socket = new ServerSocket(server_port);
		while(true){
			Socket connectionSocket = socket.accept();
        	Listener listen = new Listener(connectionSocket);
        	new Thread(listen).start();
		}
	}
	public void send(Message message) throws IOException{
		/*TODO get info of this message and check send rules */
		/*TODO get a socket from connection list, if not exist, create another socket and send message*/
		/*TODO set message content like sequence number etc. */
		String dest = message.get_dest();
		Socket fd = null;
		if( connections.get(dest) != null  ){
			fd = connections.get(dest);
		}
		else{
			InetAddress dst_ip = InetAddress.getByName(hosts.get(dest).get_ip());
			int dst_port = Integer.parseInt(hosts.get(dest).get_port());
			fd = new Socket(dst_ip,dst_port);
			connections.put(dest, fd);
		}
		ObjectOutputStream out = new ObjectOutputStream(fd.getOutputStream());

		// When user create message: should call set_seqnumber, set_dest,set_kind
		message.set_source(local_name);
		message.set_duplicate(false);
		int result = send_check(message);
		if(result == 0){
			// send the message
			out.writeObject(message);
			while( !send_queue.isEmpty()){
				send(send_queue.poll());
			}
		}
		else if(result == 1){
			// drop the message
		}
		else if(result  == 2){
			//delay the message, add to a send queue
			if( !message.get_send_delay() ){
				message.set_send_delay(true);
				send_queue.add(message);
			}
			else{
				out.writeObject(message);
			}
		}
		else if(result == 3){
			//duplicate the message
			Message dup = new Message(message);
			dup.set_duplicate(true);
			out.writeObject(message);
			out.writeObject(dup);
			while( !send_queue.isEmpty()){
				send(send_queue.poll());
			}
		}
		out.close();
	}
	public Message receive(){
		Listener p = new Listener();
		if( p.get_recv_queue().isEmpty()){
			return null;
		}
		else{
			/* need to clear the delay queue because we deliver a non-delayed message*/
			Listener.clear_delay_queue();
			return p.get_recv_queue().poll();
		}
	}
	private boolean parse_configuration(String file_name) throws FileNotFoundException{
		/* TODO how to use yaml? */
		FileInputStream file = new FileInputStream("/home/chenshuo/18842/lab0/configuration");
		Yaml yaml =new Yaml();
		Map<String, Object>  buffer = (Map<String, Object>) yaml.load(file);
		List<Map<String, Object>> host_list  = (List<Map<String, Object>>) buffer.get("configuration");
		List<Map<String, Object>> send_list  = (List<Map<String, Object>>) buffer.get("sendRules");
		List<Map<String, Object>> recv_list  = (List<Map<String, Object>>) buffer.get("receiveRules");
		for (Map<String, Object> iterator : host_list) {
			Host host= new Host();
			host.set_ip((String)iterator.get("ip"));
			host.set_name((String)iterator.get("name"));
			host.set_port((Integer)iterator.get("port"));
			hosts.put(host.get_name(), host);
		}
		for (Map<String, Object> iterator : send_list) {
			Rule rule = new Rule();
			rule.set_action((String)iterator.get("action"));
			rule.set_dest((String)iterator.get("dest"));
			rule.set_src((String)iterator.get("src"));
			rule.set_kind((String)iterator.get("kind"));
		    rule.set_seqNum((Integer)iterator.get("seqNum"));
			send_rules.add(rule);
		}
		for (Map<String, Object> iterator : recv_list) {
			Rule rule = new Rule();
			rule.set_action((String)iterator.get("action"));
			rule.set_dest((String)iterator.get("dest"));
			rule.set_src((String)iterator.get("src"));
			rule.set_kind((String)iterator.get("kind"));
			rule.set_seqNum((Integer)iterator.get("seqNum"));
			recv_rules.add(rule);
		}
		return true;
	}
	public static int send_check(Message send){
		for(Rule r:send_rules){
			boolean src = (null == r.get_src()) || ( null != r.get_src() && r.get_src().equalsIgnoreCase(send.get_src()));
			boolean dest = (null == r.get_dest()) || (null != r.get_dest() && r.get_dest().equalsIgnoreCase(send.get_dest()));
			boolean kind = (null == r.get_kind()) || (null != r.get_kind())&& r.get_kind().equalsIgnoreCase(send.get_kind());
			boolean seq = (0 == r.get_int_seqNum()) || ((0 != r.get_int_seqNum()) && r.get_int_seqNum() == send.get_int_seq());
			if(src && dest && kind && seq){
				if(r.get_action().equalsIgnoreCase("drop")) return 1;
				if(r.get_action().equalsIgnoreCase("delay")) return 2;
				if(r.get_action().equalsIgnoreCase("duplicate")) return 3;
			}
		}
		return 0;
	}
	public static int recv_check(Message recv){
		for(Rule r:recv_rules){
			boolean src = (null == r.get_src()) || ( null != r.get_src() && r.get_src().equalsIgnoreCase(recv.get_src()));
			boolean dest = (null == r.get_dest()) || (null != r.get_dest() && r.get_dest().equalsIgnoreCase(recv.get_dest()));
			boolean kind = (null == r.get_kind()) || (null != r.get_kind())&& r.get_kind().equalsIgnoreCase(recv.get_kind());
			boolean seq = (0 == r.get_int_seqNum()) || ((0 != r.get_int_seqNum()) && r.get_int_seqNum() == recv.get_int_seq());
			if(src && dest && kind && seq){
				if(r.get_action().equalsIgnoreCase("drop")) return 1;
				if(r.get_action().equalsIgnoreCase("delay")) return 2;
				if(r.get_action().equalsIgnoreCase("duplicate")) return 3;
			}
		}
		return 0;
	}
	public static void main(String[] args) throws IOException{
	/* TODO need delete, for testing */
		MessagePasser test = new MessagePasser("E:\\研究生二\\842\\config.txt","host");
	}
}