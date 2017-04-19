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
				 int length) {//�������ַ�����ݶ����ض����飬�Ƚ������ַת��Ϊ�����ַ���ٽ����ݴ��ڴ浽�ض�����
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


int transferredCounter=0;//�Ѿ���ȡ�˵���������
//vaddrΪ��ʼ�ַ���ַ
int endVAddr=vaddr+length-1;//�����ַ���ַ
int startVirtualPage=Processor.pageFromAddress(vaddr);//��ʼҳ
int endVirtualPage=Processor.pageFromAddress(endVAddr);//����ҳ
for(int i=startVirtualPage;i<=endVirtualPage;i++){//����һҳһҳ����������
if(!lookUpPageTable(i).valid){
	break;
}
int pageStartVirtualAddress=Processor.makeAddress(i, 0);//�����ַҳ�濪ʼ��
int pageEndVirtualAddress=Processor.makeAddress(i, pageSize-1);//�����ַҳ�������
int addrOffset;
int amount=0;
if(vaddr>pageStartVirtualAddress&&endVAddr<pageEndVirtualAddress){//�������С��һҳ���Ҵ���ҳ���в�
	addrOffset=vaddr-pageStartVirtualAddress;//��ַ��ʼ����ҳ�濪ʼ����ƫ����
	amount=length;//����С��һҳ��������ҳ��������Ϊ���ݳ���
}else if(vaddr<=pageStartVirtualAddress&&endVAddr<pageEndVirtualAddress){//�������һҳ��Ҫ�����ݸպô�ҳ�濪ʼ����Ҫ���Ѿ�����һҳ��
	addrOffset=0;//����Ȼ�������Ƕ��˲�ֹһҳ������ֻ��һҳ����ǡ����ҳ�濪ʼ�ˣ�ƫ������Ϊ0
	amount=endVAddr-pageStartVirtualAddress+1;//��ȡ�����һҳ����������
}else if(vaddr>pageStartVirtualAddress&&endVAddr>=pageEndVirtualAddress){//���ݲ�ֹһҳ�����ڶ����ǵ�һҳ
	addrOffset=vaddr-pageStartVirtualAddress;
	amount=pageEndVirtualAddress-vaddr+1;
}else{//������������м�ҳ��ƫ�����͸�ҳ��ȡ��������
	addrOffset=0;
	amount=pageSize;
}
int paddr=Processor.makeAddress(lookUpPageTable(i).ppn, addrOffset);//��ҳ�濪ʼ�������ַ
System.arraycopy(memory, paddr, data, offset+transferredCounter, amount);//�������ַ�ĸ�ҳ���ݶ�ȡ��������
transferredCounter+=amount;//���ϸ�ҳ��ȡ������	
}

return transferredCounter;
    }
protected TranslationEntry lookUpPageTable(int vpn) {//���������ڴ��ҵ�ҳ���ж�Ӧ����Ŀ
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
				  int length) {//�������е�����д���ڴ棬�Ƚ������ַת��Ϊ�����ַ���������е�����д�������ڴ�
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

int transferredCounter=0;//�Ѿ�д�����������
//vaddrΪд�����ݵ���ʼ��ַ
int endVAddr=vaddr+length-1;//д�����ݵĽ�����ַ
int startVirtualPage=Processor.pageFromAddress(vaddr);//��ʼ��ҳ
int endVirtualPage=Processor.pageFromAddress(endVAddr);//������ҳ
for(int i=startVirtualPage;i<=endVirtualPage;i++){//��ҳд��
	if(!lookUpPageTable(i).valid||lookUpPageTable(i).readOnly){//�����ҳֻ������Ч���˳�
		break;
	}
	int pageStartVirtualAddress=Processor.makeAddress(i, 0);//ҳ�濪ʼ��
	int pageEndVirtualAddress=Processor.makeAddress(i, pageSize-1);//ҳ�������
	int addrOffset;//ƫ�Ƶ�ַ
	int amount=0;//��ҳд������
	if(vaddr>pageStartVirtualAddress&&endVAddr<pageEndVirtualAddress){//���ֻ�в���һҳ������Ҫд�룬��������ҳ���м�
		addrOffset=vaddr-pageStartVirtualAddress;//ƫ�Ƶ�ַΪд�����ݵ��׵�ַ-ҳ��Ŀ�ʼ��ַ��
		amount=length;//��ҳд������
	}else if(vaddr>pageStartVirtualAddress&&endVAddr>=pageEndVirtualAddress){//���д���Ϊ��һҳ�����������һҳ
		addrOffset=vaddr-pageStartVirtualAddress;//ƫ�Ƶ�ַͬ��
		amount=pageEndVirtualAddress-vaddr+1;//д����������ҳ��ĩβ-д�����ݵ��׵�ַ+1
	}else if(vaddr<=pageStartVirtualAddress&&endVAddr<pageEndVirtualAddress){//д��������һҳ����������ǵ�һҳ��Ҳ�п����ǵ�һҳ��ҳ���׵�ַ��д�������׵�ַ�غϡ�
		addrOffset=0;//ƫ�Ƶ�ַΪ0
		amount=endVAddr-pageStartVirtualAddress+1;//��ҳд������Ϊд�����ݵ�β��ַ-��ҳ���׵�ַ+1
	}else{//����д������м�ҳ
		addrOffset=0;
		amount=pageSize;
	}
	int paddr=
			Processor.makeAddress(lookUpPageTable(i).ppn, addrOffset);//�õ���ҳд�����ݵĿ�ʼ�����ַ
	System.arraycopy(data, offset+transferredCounter, memory, paddr, amount);//�������е�����д���ڴ�
	transferredCounter+=amount;//�Ѿ�д�������+��ҳд�������
}


return transferredCounter;//����ʵ��д�������	
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
    protected boolean loadSections() {//�����е�ҳװ���������ڴ��У��������ڴ棬�����װ��
//	if (numPages > Machine.processor().getNumPhysPages()) {//��������ҳ�����������ڴ�
//	    coff.close();
//	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
//	    return false;
//	}

//allocate pages to Process
UserKernel.allocateMemoryLock.acquire();
if (numPages >UserKernel.availablePages.size()) {//��������ҳ�����������ڴ�
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
	    CoffSection section = coff.getSection(s);//��ÿ�
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
	    	int vpn = section.getFirstVPN()+i;//��ÿ�ĵ�һҳ
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
    protected void unloadSections() {//�ͷ���Դ
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
	coff.close();//�ر�coff
	for(int i = 0;i<16;i++){//�رմ򿪵��ļ�
		if(descriptors[i++]!=null)
			handleClose(i);
	}
	this.status = status;
	unloadSections();//�ͷ��ڴ���Դ
	int childrenNum=children.size();
	for(int i=0;i<childrenNum;i++){//�����ӽ��̵ĸ����ÿ�
		UserProcess child=children.removeFirst();
		child.parent=null;
	}
	
	if(numOfRunningProcess==1){//���ֻʣһ�����̣���ر�nachos
		Kernel.kernel.terminate();
	}else{
		numOfRunningProcess--;
		UThread.finish();//������ǰ����
	}
}

private int handleExec(int nameVAddr,int argsNum,int argsVAddr ){//nameVAddrΪִ�г���ĵ�ַ��argsnumΪ������Ŀ��argsVAddrΪ����ָ��
	if(nameVAddr<0||argsNum<0||argsVAddr<0){
		System.out.println("handleExec:Invalid parameter");
		return -1;
	}
	String fileName=readVirtualMemoryString(nameVAddr, 256);//��ȡ�ļ�����
	if(fileName==null){
		System.out.println("handleExec:Read filename failed");
		return -1;
	}
	if(!fileName.contains(".coff")){//����Ϊ��׺.coff�ļ�
		System.out.println("handleExec:Filename should end with .coff");
		return -1;
	}
	String[] args=new String[argsNum];//newһ������
	for(int i=0;i<argsNum;i++){//���ȡ��������������args��
		byte[] buffer=new byte[4];//��ַΪ32λ������Ϊ�ĸ��ֽ�
		int readLength;
		readLength=readVirtualMemory(argsVAddr+i*4,buffer);//������һ��������������buffer
		if(readLength!=4){
			System.out.println("handleExec:Read argument address falied");
			return -1;
		}
		int argVAddr=Lib.bytesToInt(buffer, 0);//���������ַ�����ת��Ϊint�����ַ�����ַ��
		String arg=readVirtualMemoryString(argVAddr,256);//������ַ��ָ���ַ���
		if(arg==null){
			System.out.println("handleExec:Read argument failed");
			return -1;
		}
		args[i]=arg;//���������ַ�������args��
	}
	UserProcess child=UserProcess.newUserProcess();//�������ӽ���
	boolean isSuccessful=child.execute(fileName, args);//ִ�н���
	if(!isSuccessful){
		System.out.println("handleExec:Execute child process failed");
		return -1;
	}
	child.parent=this;//���׵��ڵ�ǰ����
	this.children.add(child);
	return child.PID;
}

private int handleJoin(int PID,int statusVAddr){
	if(PID<0||statusVAddr<0){//��������ڸý��̣�����-1
		return -1;
	}
	UserProcess child=null;
	int childrenNum=children.size();//������Ŀ
	for(int i=0;i<childrenNum;i++){//�ҵ�pid��ָ�ĺ���
		if(children.get(i).PID==PID){
			child=children.get(i);
			break;
		}
	}
	
	if(child==null){//���pidû��ָ��ǰ���̵��ӽ��̣��򷵻�-1
		System.out.println("handleJoin:this is not the child");
		return -1;
	}
	child.thread.join();//�����Ӱ󶨵��̵߳���join����

	child.parent=null;
	children.remove(child);
	Integer status=childrenExitStatus.get(child.PID);//�õ�������ӵ�״̬�������˳�״̬��Ϊ0
	if((int)(status)!=0){//�����Ϊ0�����ӽ����쳣�˳�������������
		System.out.println("handleJoin:Cannot find the exit status of the child");
		return 0;
	}else{//����״̬д���ڴ��е�ַΪ����statusAddr��
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

private int handleCreate(int fileAddress){//�����ļ�
if(fileAddress<0){
	Lib.debug(dbgProcess, "handleCreate:Invalid virtual address");
	return -1;
}
String fileName=readVirtualMemoryString(fileAddress,256);//���ڴ��д��ļ�������
if(fileName==null){
	System.out.println("Read filename failed");
	return -1;
}
int availableIndex=-1;
for(int i=0;i<16;i++){//ÿ����������ʮ�����ļ�
	if(descriptors[i]==null){
		availableIndex=i;
		break;
	}
}
if(availableIndex==-1){
	System.out.println("Cannot create more than 16 files");
	return -1;
}else{//���ļ�������������򴴽�
	OpenFile file=ThreadedKernel.fileSystem.open(fileName, true);//���ļ�����������ڣ�����һ�����ļ�
	if(file==null){
System.out.println("Create failed");
		return-1;
	}else{
		descriptors[availableIndex]=file;
		return availableIndex;
	}		
}	
}

private int handleOpen(int fileAddress){//���ļ�
	if(fileAddress<0){//�ļ���ַ����С����
		Lib.debug(dbgProcess, "handleCreate:Invalid virtual address");
		return -1;
	}
	String fileName=readVirtualMemoryString(fileAddress,256);//���ڴ��н��ļ�������
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
		OpenFile file=ThreadedKernel.fileSystem.open(fileName, false);//���ļ�ϵͳ�д��ļ�����������ڷ���null
		if(file==null){
	System.out.println("Create failed");
			return-1;
		}else{
			descriptors[availableIndex]=file;
			return availableIndex;
		}		
	}	
}

private int handleRead(int descriptor,int bufferVAddr,int size){//���ļ����Ƚ��ļ��е�����д�����飬�ٽ������е�����д���ڴ�
	if(descriptor<0||descriptor>15){//�ļ�������ֻ��0~15
		System.out.println("Descriptor out of range");
		return -1;
	}
	if(size<0){//��ȡ��������С����
		System.out.println("Size to read cannot be negative");
		return -1;
	}
	OpenFile file;
	if(descriptors[descriptor]==null){//�����ڸ��ļ�
		System.out.println("File doesn't exist in the descriptor table");
		return -1;
	}else{
		file=descriptors[descriptor];
	}
	int length=0;
	byte[] reader=new byte[size];
length=file.read(reader, 0, size);//�����ݴ��ļ��ж��뵽�����У������ض�ȡ��ʵ�ʳ��ȣ��ú���ʵ����OpenFile���������
	if(length==-1){//��ȡ���ɹ�
		System.out.println("Error occurred when try to read file");
		return -1;
	}
	int count=0;
	count=writeVirtualMemory(bufferVAddr,reader,0,length);//������д���ڴ�
	return count;

}

private int handleWrite(int descriptor,int bufferVAddr,int size){//д�ļ������ڴ��е�������д�����飬�ٽ������е�����д���ļ�
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
	length=readVirtualMemory(bufferVAddr,writer,0,size);//���ڴ��н����ݴ���������
	int count=0;
	count=file.write(writer, 0, length);
	//System.out.println(size==count);
	if(count==-1){
		Lib.debug(dbgProcess, "handleWrite:Error occur when read file");
		return -1;
	}
	return count;
}

private int handleClose(int descriptor){//�ر��ļ�
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

private int handleUnlink(int vaddr){//ɾ���ļ�
	if(vaddr<0){
		Lib.debug(dbgProcess, "handleUnlink:Invalid virtual address");
		return -1;
	}
	String fileName=readVirtualMemoryString(vaddr,256);//���ڴ��ж����ļ�����
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
