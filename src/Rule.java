public class Rule {
	private String type;
	private String action;
	private String src;
	private String dest;
	private String kind;
	private String seqNum;
	public Rule(){
		type = action = src = dest = kind = seqNum = null;
	}
	public boolean set_type(String type){
		if( ! (type.equals("send") || type.equals("recv")) ){
			System.out.println("error rule type");
			return false;
		}
		this.type = type;
		return true;
	}
	public void set_action(String action){
		this.action = action;
	}
	public void set_src(String src){
		this.src = src;
	}
	public void set_dest(String dest){
		this.dest = dest;
	}
	public void set_kind(String kind){
		this.kind = kind;
	}
	public void set_seqNum(String seq){
		this.seqNum = seq;
	}
	public void set_seqNum(Integer seq){
		this.seqNum = String.valueOf(seq);
	}
	public void show(){
		System.out.print("src: " + this.src + "\ndest: "+this.dest + "\naction: "+ this.action + "\nseq: "+ this.seqNum + "\n\n");
	}
	public int get_int_seqNum(){return Integer.parseInt(this.seqNum);}
	public String get_type(){return this.type;}
	public String get_action(){return this.action;}
	public String get_src(){return this.src;}
	public String get_dest(){return this.dest;}
	public String get_kind(){return this.kind;}
	public String get_seqNum(){return this.seqNum;}
}