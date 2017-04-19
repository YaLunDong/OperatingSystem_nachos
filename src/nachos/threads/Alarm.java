package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {

boolean preState = Machine.interrupt().disable();//关中断
WaitForAlarmThread x;
for(java.util.Iterator i = waitForAlarmThreadList.iterator();i.hasNext();){
	x = (WaitForAlarmThread)i.next();//取出链表中的每个线程判断是否达到唤醒时间
	if(x.wakeTime<=Machine.timer().getTime()){//如果达到唤醒时间，将其从链表中移除并唤醒该线程
		i.remove();
		x.thread.ready();
	}
}
Machine.interrupt().restore(preState);//恢复中断
	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	
    //long wakeTime = Machine.timer().getTime() + x;
	//while (wakeTime > Machine.timer().getTime())
    //KThread.yield();
boolean preState = Machine.interrupt().disable();//关中断
long wakeTime = Machine.timer().getTime()+x;//计算唤醒的时间
WaitForAlarmThread waitForAlarmThread = new WaitForAlarmThread(wakeTime, KThread.currentThread());
waitForAlarmThreadList.add(waitForAlarmThread);//将线程加入到等待链表中
KThread.sleep();//让该线程睡眠
Machine.interrupt().restore(preState);//恢复中断   
    }
    
class WaitForAlarmThread{
	long wakeTime;
	KThread thread;
	public WaitForAlarmThread(long wakeTime,KThread thread){
		this.wakeTime=wakeTime;
		this.thread=thread;
	}
}

private static LinkedList<WaitForAlarmThread> waitForAlarmThreadList = new LinkedList<WaitForAlarmThread>(); 
public static void AlarmTest(){
	KThread a = new KThread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			System.out.println("线程1启动");
			for(int i = 0;i<5;i++){
				if(i == 2){
					System.out.println("线程1要暂时隐退，此时时间为："+Machine.timer().getTime()+",大约800clock ticks之后再见");
					new Alarm().waitUntil(800);
					System.out.println("线程1回来了，此时时间为："+Machine.timer().getTime());
				}
				System.out.println("*** thread 1 looped "
						   + i + " times");
				KThread.currentThread().yield();
			}
		}
	});
	a.fork();
	for(int i = 0;i<5;i++){
			if(i == 2){
				System.out.println("线程0要暂时隐退，此时时间为："+Machine.timer().getTime()+",大约1700clock ticks之后再见");
				new Alarm().waitUntil(1700);
				System.out.println("线程0回来了，此时时间为："+Machine.timer().getTime());
			}
		System.out.println("*** thread 0 looped "
				   + i + " times");
		KThread.currentThread().yield();
	}
}
}
