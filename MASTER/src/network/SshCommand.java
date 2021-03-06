package network;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


public class SshCommand extends Thread{
	
	// Network config
	private NetworkConfig networkConfig;
	
	// Other
	private String machine;
	private String command;
	private Boolean sshTest;

	// Reponse
	private ArrayBlockingQueue<String> standard_output = new ArrayBlockingQueue<String>(1000);
	private ArrayBlockingQueue<String> error_output = new ArrayBlockingQueue<String>(1000);
	private ArrayList<String> response;
	private boolean connection_status = false;

	public String getMachine() {
		return machine;
	}

	public void setConnectionOK(boolean connection_status) {
		this.connection_status = connection_status;
	}

	public boolean isConnectionOK() {
		return connection_status;
	}

	public SshCommand(NetworkConfig networkConfig, String machine, String command, boolean sshTest){
		this.sshTest = sshTest;
		this.machine = machine;
		this.networkConfig = networkConfig;
		if (sshTest){
			this.command = "echo OK";
		}
		else {
			this.command = command;
		}
		this.response = new ArrayList<String>();
	}
	
	public void show(String texte){
		System.out.println("[SSH "+machine+"] "+texte);
	}
	
	public void run(){
		Integer timeout;
		if (this.sshTest){
			timeout = this.networkConfig.test_timeout;
		}
		else{
			timeout = this.networkConfig.timeout;
		}
	 try {
         String[] fullCommand = {
        		 "ssh",
        		 "-i",this.networkConfig.sshKeyPath,
        		 "-o","StrictHostKeyChecking=no",
        		 this.networkConfig.sshUser+"@"+this.machine, 
        		 this.command};
            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            Process p = pb.start();
            StreamReader fluxSortie = new StreamReader(p.getInputStream(), standard_output);
            StreamReader fluxErreur = new StreamReader(p.getErrorStream(), error_output);

            new Thread(fluxSortie).start();
            new Thread(fluxErreur).start();

            String s = standard_output.poll(timeout, TimeUnit.SECONDS);
            while(s!=null && !s.equals("ENDOFTHREAD")){
            	
            	if (this.sshTest){
            		if(s.contains("OK")){
                		connection_status = true;
                	}
            	}
            	// affiche(s);

            	this.response.add(s);
            	s = standard_output.poll(this.networkConfig.timeout, TimeUnit.SECONDS);
            }
            
            s = null;
            s = error_output.poll(timeout, TimeUnit.SECONDS);
            while(s!=null && !s.equals("ENDOFTHREAD")){
            	if (! this.sshTest){
            		// do not print errors for ssh tests
                	show(s);
            	}
            	this.response.add(s);
            	s = error_output.poll(this.networkConfig.timeout, TimeUnit.SECONDS);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
	
	public ArrayList<String> get_response(){
		return this.response;
	}
	public String get_command(){
		return this.command;
	}
}
