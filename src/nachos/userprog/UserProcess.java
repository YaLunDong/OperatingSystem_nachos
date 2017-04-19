package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.jws.soap.SOAPBinding.Use;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	//pageTable = new TranslationEntry[numPhysPages];
	//for (int i=0; i<numPhysPages; i++)
	//   pageTable[i] = new TranslationEntry(i,i,,false,false,false);

stdin = UserKernel.console.openForReading();
stdout = UserKernel.console.openForWriting();

PID = counter++;
descriptors = new OpenFile[16];
descriptors[0] = stdin;
descriptors[1] = stdout;
parent=null;
children=new LinkedList<UserProcess>();
childrenExitStatus = new HashMap<Integer, Integer>();

    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	//new UThread(this).setName(name).fork();
thread = (UThread) new UThread(this).setName(name);
thread.fork();
numOfRunningProcess++;
	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {//将虚拟地址的内容读到特定数组，先将虚拟地址转换为物理地址，再将数据从内存到特定数组
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	//if (vaddr < 0 || vaddr >= memory.length)
	//    return 0;

	//int amount = Math.min(length, memory.length-vaddr);
	//System.arraycopy(memory, vaddr, data, offset, amount);

	//return amount;
	
	
	if (vaddr < 0 || vaddr +length-1>Machine.processor().makeAddress(numPages-1, pageSize-1)){
		Lib.debug(dbgProcess, "readVirtualMemory:Invalid virtual Address");
		return 0;
	}


int transferredCounter=0;//已经读取了的内容数量
//vaddr为开始字符地址
int endVAddr=vaddr+length-1;//结束字符地址
int startVirtualPage=Processor.pageFromAddress(vaddr);//开始页
int endVirtualPage=Processor.pageFromAddress(endVAddr);//结束页
for(int i=startVirtualPage;i<=endVirtualPage;i++){//数据一页一页读到数组中
if(!lookUpPageTable(i).valid){
	break;
}
int pageStartVirtualAddress=Processor.makeAddress(i, 0);//虚拟地址页面开始处
int pageEndVirtualAddress=Processor.makeAddress(i, pageSize-1);//虚拟地址页面结束处
int addrOffset;
int amount=0;
if(vaddr>pageStartVirtualAddress&&endVAddr<pageEndVirtualAddress){//如果内容小于一页，且处于页面中部
	addrOffset=vaddr-pageStartVirtualAddress;//地址开始处离页面开始处的偏移量
	amount=length;//内容小于一页，所以这页读的数量为内容长度
}else if(vaddr<=pageStartVirtualAddress&&endVAddr<pageEndVirtualAddress){//读了最后一页【要不内容刚好从页面开始处，要不已经读了一页】
	addrOffset=0;//很显然，不管是读了不止一页，还是只有一页内容恰好在页面开始端，偏移量均为0
	amount=endVAddr-pageStartVirtualAddress+1;//读取的最后一页的内容数量
}else if(vaddr>pageStartVirtualAddress&&endVAddr>=pageEndVirtualAddress){//内容不止一页，现在读的是第一页
	addrOffset=vaddr-pageStartVirtualAddress;
	amount=pageEndVirtualAddress-vaddr+1;
}else{//否则读到的是中间页，偏移量和该页读取数量如下
	addrOffset=0;
	amount=pageSize;
}
int paddr=Processor.makeAddress(lookUpPageTable(i).ppn, addrOffset);//该页面开始的物理地址
System.arraycopy(memory, paddr, data, offset+transferredCounter, amount);//将物理地址的该页内容读取到数组中
transferredCounter+=amount;//加上该页读取的数量	
}

return transferredCounter;
    }
protected TranslationEntry lookUpPageTable(int vpn) {//根据虚拟内存找到页表中对应的条目
    if (pageTable == null)
        return null;

    if (vpn >= 0 && vpn < pageTable.length)
        return pageTable[vpn];
    else
        return null;
}
    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {//将数组中的内容写入内存，先将虚拟地址转换为物理地址，再数组中的内容写入虚拟内存
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
//	// for now, just assume that virtual addresses equal physical addresses
//	if (vaddr < 0 || vaddr >= memory.length)
//	    return 0;
//
//	int amount = Math.min(length, memory.length-vaddr);
//	System.arraycopy(data, offset, memory, vaddr, amount);
//
//	return amount;
	
	
if (vaddr < 0 || vaddr +length-1>Processor.makeAddress(numPages-1, pageSize-1)){
	Lib.debug(dbgProcess, "writeMemory:Invalid virtual address");
	return 0;
}

int transferredCounter=0;//已经写入的内容数量
//vaddr为写入内容的起始地址
int endVAddr=vaddr+length-1;//写入内容的结束地址
int startVirtualPage=Processor.pageFromAddress(vaddr);//开始的页
int endVirtualPage=Processor.pageFromAddress(endVAddr);//结束的页
for(int i=startVirtualPage;i<=endVirtualPage;i++){//逐页写入
	if(!lookUpPageTable(i).valid||lookUpPageTable(i).readOnly){//如果该页只读或无效则退出
		break;
	}
	int pageStartVirtualAddress=Processor.makeAddress(i, 0);//页面开始处
	int pageEndVirtualAddress=Processor.makeAddress(i, pageSize-1);//页面结束处
	int addrOffset;//偏移地址
	int amount=0;//该页写入数量
	if(vaddr>pageStartVirtualAddress&&endVAddr<pageEndVirtualAddress){//如果只有不足一页内容需要写入，且内容在页面中间
		addrOffset=vaddr-pageStartVirtualAddress;//偏移地址为写入内容的首地址-页面的开始地址处
		amount=length;//该页写入数量
	}else if(vaddr>pageStartVirtualAddress&&endVAddr>=pageEndVirtualAddress){//如果写入的为第一页，但不是最后一页
		addrOffset=vaddr-pageStartVirtualAddress;//偏移地址同上
		amount=pageEndVirtualAddress-vaddr+1;//写入数量等于页面末尾-写入内容的首地址+1
	}else if(vaddr<=pageStartVirtualAddress&&endVAddr<pageEndVirtualAddress){//写入的是最后一页，大多数不是第一页，也有可能是第一页【页面首地址和写入内容首地址重合】
		addrOffset=0;//偏移地址为0
		amount=endVAddr-pageStartVirtualAddress+1;//该页写入数量为写入内容的尾地址-该页的首地址+1
	}else{//否则写入的是中间页
		addrOffset=0;
		amount=pageSize;
	}
	int paddr=
			Processor.makeAddress(lookUpPageTable(i).ppn, addrOffset);//得到该页写入内容的开始物理地址
	System.arraycopy(data, offset+transferredCounter, memory, paddr, amount);//将数组中的内容写入内存
	transferredCounter+=amount;//已经写入的数量+该页写入的数量
}


return transferredCounter;//返回实际写入的数量	
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {//将块中的页装载至物理内存中：先申请内存，再逐块装载
//	if (numPages > Machine.processor().getNumPhysPages()) {//程序所需页数大于物理内存
//	    coff.close();
//	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
//	    return false;
//	}

//allocate pages to Process
UserKernel.allocateMemoryLock.acquire();
if (numPages >UserKernel.availablePages.size()) {//程序所需页数大于物理内存
    coff.close();
    Lib.debug(dbgProcess, "\tinsufficient physical memory");
    UserKernel.allocateMemoryLock.release();
    return false;
}

pageTable = new TranslationEntry[numPages];
for(int i =0;i<numPages;i++){
	int Page = UserKernel.availablePages.remove();
	pageTable[i] = new TranslationEntry(i,Page,true,false,false,false);
}
UserKernel.allocateMemoryLock.release();

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);//获得块
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
	    	int vpn = section.getFirstVPN()+i;//获得块的第一页
pageTable[vpn].readOnly = section.isReadOnly();	
section.loadPage(i,pageTable[vpn].ppn);
		// for now, just assume virtual addresses=physical addresses
		//section.loadPage(i, vpn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {//释放资源
for (int i = 0; i < pageTable.length; ++i)
     if (pageTable[i].valid) {
    	 UserKernel.allocateMemoryLock.acquire();
         UserKernel.availablePages.add(new Integer(pageTable[i].ppn));
         UserKernel.allocateMemoryLock.release();
         pageTable[i] = new TranslationEntry(pageTable[i].vpn, 0, false, false, false, false);
     }
numPages = 0;

    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
if(PID == 0)
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
    
private void handleExit(int status){
	parent.childrenExitStatus.put(this.PID, status);
	coff.close();//关闭coff
	for(int i = 0;i<16;i++){//关闭打开的文件
		if(descriptors[i++]!=null)
			handleClose(i);
	}
	this.status = status;
	unloadSections();//释放内存资源
	int childrenNum=children.size();
	for(int i=0;i<childrenNum;i++){//将孩子进程的父亲置空
		UserProcess child=children.removeFirst();
		child.parent=null;
	}
	
	if(numOfRunningProcess==1){//如果只剩一个进程，则关闭nachos
		Kernel.kernel.terminate();
	}else{
		numOfRunningProcess--;
		UThread.finish();//结束当前进程
	}
}

private int handleExec(int nameVAddr,int argsNum,int argsVAddr ){//nameVAddr为执行程序的地址，argsnum为参数数目，argsVAddr为参数指针
	if(nameVAddr<0||argsNum<0||argsVAddr<0){
		System.out.println("handleExec:Invalid parameter");
		return -1;
	}
	String fileName=readVirtualMemoryString(nameVAddr, 256);//读取文件名称
	if(fileName==null){
		System.out.println("handleExec:Read filename failed");
		return -1;
	}
	if(!fileName.contains(".coff")){//必须为后缀.coff文件
		System.out.println("handleExec:Filename should end with .coff");
		return -1;
	}
	String[] args=new String[argsNum];//new一个数组
	for(int i=0;i<argsNum;i++){//逐个取出参数，并存入args中
		byte[] buffer=new byte[4];//地址为32位，所以为四个字节
		int readLength;
		readLength=readVirtualMemory(argsVAddr+i*4,buffer);//读出第一个参数存入数组buffer
		if(readLength!=4){
			System.out.println("handleExec:Read argument address falied");
			return -1;
		}
		int argVAddr=Lib.bytesToInt(buffer, 0);//将参数由字符数组转变为int【即字符串地址】
		String arg=readVirtualMemoryString(argVAddr,256);//读出地址所指的字符串
		if(arg==null){
			System.out.println("handleExec:Read argument failed");
			return -1;
		}
		args[i]=arg;//将读出的字符串窜入args中
	}
	UserProcess child=UserProcess.newUserProcess();//建立孩子进程
	boolean isSuccessful=child.execute(fileName, args);//执行进程
	if(!isSuccessful){
		System.out.println("handleExec:Execute child process failed");
		return -1;
	}
	child.parent=this;//父亲等于当前进程
	this.children.add(child);
	return child.PID;
}

private int handleJoin(int PID,int statusVAddr){
	if(PID<0||statusVAddr<0){//如果不存在该进程，返回-1
		return -1;
	}
	UserProcess child=null;
	int childrenNum=children.size();//孩子数目
	for(int i=0;i<childrenNum;i++){//找到pid所指的孩子
		if(children.get(i).PID==PID){
			child=children.get(i);
			break;
		}
	}
	
	if(child==null){//如果pid没有指向当前进程的子进程，则返回-1
		System.out.println("handleJoin:this is not the child");
		return -1;
	}
	child.thread.join();//将孩子绑定的线程调用join方法

	child.parent=null;
	children.remove(child);
	Integer status=childrenExitStatus.get(child.PID);//得到这个孩子的状态，正常退出状态下为0
	if((int)(status)!=0){//如果不为0，则子进程异常退出，函数返回零
		System.out.println("handleJoin:Cannot find the exit status of the child");
		return 0;
	}else{//否则将状态写入内存中地址为参数statusAddr中
		byte[] buffer=new byte[4];
		buffer=Lib.bytesFromInt(status);
		int count=writeVirtualMemory(statusVAddr,buffer);
		if(count==4){
			return 1;
		}else{
			System.out.println("handleJoin:Write status failed");
			return 0;
		}
	}
}

private int handleCreate(int fileAddress){//创建文件
if(fileAddress<0){
	Lib.debug(dbgProcess, "handleCreate:Invalid virtual address");
	return -1;
}
String fileName=readVirtualMemoryString(fileAddress,256);//从内存中从文件名读出
if(fileName==null){
	System.out.println("Read filename failed");
	return -1;
}
int availableIndex=-1;
for(int i=0;i<16;i++){//每个进程最多打开十六个文件
	if(descriptors[i]==null){
		availableIndex=i;
		break;
	}
}
if(availableIndex==-1){
	System.out.println("Cannot create more than 16 files");
	return -1;
}else{//打开文件，如果不存在则创建
	OpenFile file=ThreadedKernel.fileSystem.open(fileName, true);//打开文件，如果不存在，创建一个新文件
	if(file==null){
System.out.println("Create failed");
		return-1;
	}else{
		descriptors[availableIndex]=file;
		return availableIndex;
	}		
}	
}

private int handleOpen(int fileAddress){//打开文件
	if(fileAddress<0){//文件地址不能小于零
		Lib.debug(dbgProcess, "handleCreate:Invalid virtual address");
		return -1;
	}
	String fileName=readVirtualMemoryString(fileAddress,256);//从内存中将文件名读出
	if(fileName==null){
		System.out.println("Read filename failed");
		return -1;
	}
	int availableIndex=-1;
	for(int i=0;i<16;i++){
		if(descriptors[i]==null){
			availableIndex=i;
			break;
		}
	}
	if(availableIndex==-1){
		System.out.println("Cannot create more than 16 files");
		return -1;
	}else{
		OpenFile file=ThreadedKernel.fileSystem.open(fileName, false);//从文件系统中打开文件，如果不存在返回null
		if(file==null){
	System.out.println("Create failed");
			return-1;
		}else{
			descriptors[availableIndex]=file;
			return availableIndex;
		}		
	}	
}

private int handleRead(int descriptor,int bufferVAddr,int size){//读文件，先将文件中的内容写入数组，再将数组中的内容写入内存
	if(descriptor<0||descriptor>15){//文件描述符只有0~15
		System.out.println("Descriptor out of range");
		return -1;
	}
	if(size<0){//读取数量不能小于零
		System.out.println("Size to read cannot be negative");
		return -1;
	}
	OpenFile file;
	if(descriptors[descriptor]==null){//不存在该文件
		System.out.println("File doesn't exist in the descriptor table");
		return -1;
	}else{
		file=descriptors[descriptor];
	}
	int length=0;
	byte[] reader=new byte[size];
length=file.read(reader, 0, size);//将内容从文件中读入到数组中，并返回读取的实际长度，该函数实现在OpenFile类的子类中
	if(length==-1){//读取不成功
		System.out.println("Error occurred when try to read file");
		return -1;
	}
	int count=0;
	count=writeVirtualMemory(bufferVAddr,reader,0,length);//从数组写入内存
	return count;

}

private int handleWrite(int descriptor,int bufferVAddr,int size){//写文件，将内存中的内容先写入数组，再将数组中的内容写入文件
	if(descriptor<0||descriptor>15){
		Lib.debug(dbgProcess,"hanleWirte:Descriptor out of range");
		return -1;
	}
	if(size<0){
		Lib.debug(dbgProcess, "handleWrite:Size to write cannot be negative");
		return -1;	
	}
	OpenFile file;
	if(descriptors[descriptor]==null){
		Lib.debug(dbgProcess, "handleWrite:File doesn't exist in the descriptor table");
		return -1;
	}else{
		file=descriptors[descriptor];
	}
	int length=0;
	byte[] writer=new byte[size];
	length=readVirtualMemory(bufferVAddr,writer,0,size);//从内存中将内容存入数组中
	int count=0;
	count=file.write(writer, 0, length);
	//System.out.println(size==count);
	if(count==-1){
		Lib.debug(dbgProcess, "handleWrite:Error occur when read file");
		return -1;
	}
	return count;
}

private int handleClose(int descriptor){//关闭文件
	if(descriptor<0||descriptor>15){
		Lib.debug(dbgProcess, "handleClose:Descriptor out of range");
		return -1;
	}
	if(descriptors[descriptor]==null){
		Lib.debug(dbgProcess, "handleClose:File doesn't exist in the descriptor table");
		return -1;
	}else{
		descriptors[descriptor].close();
		descriptors[descriptor]=null;
	}
	return 0;
}

private int handleUnlink(int vaddr){//删除文件
	if(vaddr<0){
		Lib.debug(dbgProcess, "handleUnlink:Invalid virtual address");
		return -1;
	}
	String fileName=readVirtualMemoryString(vaddr,256);//从内存中读出文件名字
	if(fileName==null){
		Lib.debug(dbgProcess, "handleUnlink:Read filename failed");
		return -1;
	}
	OpenFile file;
	int index=-1;
	for(int i=0;i<16;i++){
		file=descriptors[i];
		if(file!=null&&file.getName().compareTo(fileName)==0){
			index=i;
			break;
		}
	}	
	if(index!=-1){
		Lib.debug(dbgProcess, "handleUnlink:File should be closed first");
		return -1;
	}
	boolean isSuccessful=ThreadedKernel.fileSystem.remove(fileName);
	if(!isSuccessful){
		Lib.debug(dbgProcess, "handleUnlink:Remove failed");
		return -1;
	}

	return 0;


}

    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	    
	case syscallCreate:
        return handleCreate(a0);

    case syscallOpen:
        return handleOpen(a0);

    case syscallRead:
        return handleRead(a0, a1, a2);

    case syscallWrite:
        return handleWrite(a0, a1, a2);

    case syscallClose:
        return handleClose(a0);

    case syscallUnlink:
        return handleUnlink(a0);

    case syscallExec:
        return handleExec(a0, a1, a2);

    case syscallJoin:
        return handleJoin(a0, a1);

    case syscallExit:
        handleExit(a0);
        return 0;
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    
protected UserProcess parent;
protected LinkedList<UserProcess> children;    
protected OpenFile[] descriptors;
protected int PID;
protected static int counter = 0;
protected HashMap<Integer,Integer> childrenExitStatus;
protected OpenFile stdin;
protected OpenFile stdout;
private int status;
private UThread thread;
private static int numOfRunningProcess = 1;
}
