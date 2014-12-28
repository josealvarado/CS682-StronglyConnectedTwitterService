
public class MultiReaderLock {

	// Private member variables
	private int readers;
	private int writers;
	
	/**
	 * Default constructor
	 */
	public MultiReaderLock(){
		readers = 0;
		writers = 0;
	}
	
	/**
	 * Secures the lock when being read
	 */
	public synchronized void lockRead(){
		while(writers > 0){
			try{
				this.wait();
			} catch(InterruptedException e){
				System.out.println("Interruted Exception! " + e);
			}
		}
		readers++;
	}
	
	/**
	 * Releases the lock when being read
	 */
	public synchronized void unlockRead(){
		readers--;
		this.notifyAll();
	}
	
	/**
	 * Secures the lock when being written
	 */
	public synchronized void lockWrite(){
		while(writers > 0 || readers > 0){
			try{
				this.wait();
			} catch(InterruptedException e){
				System.out.println("Interruted Exception! " + e);
			}
		}
		writers++;
	}
	
	/**
	 * Releases the lock when being written
	 */
	public synchronized void unlockWrite(){
		writers--;
		this.notifyAll();
	}

}
