package com.archer.framework.base.async;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.archer.tools.bytecode.ClassBytecode;
import com.archer.tools.bytecode.ClassEnd;
import com.archer.tools.bytecode.InstructionTable;
import com.archer.tools.bytecode.MemberInfo;
import com.archer.tools.bytecode.MemberInfo.AttributeInfo;
import com.archer.tools.bytecode.MemberInfo.CodeAttribute;
import com.archer.tools.bytecode.MemberInfo.LineNumAttribute;
import com.archer.tools.bytecode.MemberInfo.LocalVarAttribute;
import com.archer.tools.bytecode.constantpool.ConstantClass;
import com.archer.tools.bytecode.constantpool.ConstantInfo;
import com.archer.tools.bytecode.constantpool.ConstantMemberRef;
import com.archer.tools.bytecode.constantpool.ConstantNameAndType;
import com.archer.tools.bytecode.constantpool.ConstantPool;
import com.archer.tools.bytecode.constantpool.ConstantUtf8;


public class AsyncProxy {
	
	private static final String ASYNC_TYPE = "Lcom/archer/framework/base/annotation/Async;";
	private static final String THREADPOOL_CLASS = "com/archer/framework/base/async/AsyncPool";
	private static final String THREADPOOL_TYPE = "Lcom/archer/framework/base/async/AsyncPool;";
	private static final String POOL_NAME = "pool";
	private static final String SUBMIT_TASK_CLASS = "com/archer/framework/base/async/AsyncTask";
	private static final String SUBMIT_TASK_TYPE = "Lcom/archer/framework/base/async/AsyncTask;";
	private static final String OBJECT_CLASS = "java/lang/Object";
	private static final String THIS_NAME = "this";
	private static final String TASK_FIELD_NAME = "ins";
	
	
	
	private ClassBytecode superCls;
	private String superClassName;
	private MemberInfo[] superMethods;
	private ConstantInfo[] supercp;
	private String className;
	private String classTypeName;
	private String taskClassName;
	
	
	public AsyncProxy(ClassBytecode superCls) {
		this.superCls = superCls;
		this.superClassName = superCls.getClassName();
		this.superMethods = superCls.getMethods();
		this.supercp = superCls.getConstantPool().getCpInfo();
		this.className = superClassName + "$ArcherProxy";
		this.classTypeName = "L"+this.className+";";
		this.taskClassName = superClassName + "$ArcherTask";
	}
	
	public Class<?> newAsyncClass() {
		int off = 0;
		String[] overrideMethodNames = new String[superMethods.length];
		String[] overrideMethodDescs = new String[superMethods.length];
		for(int i = 0; i < superMethods.length; i++) {
			MemberInfo m = superMethods[i];
			if("RuntimeVisibleAnnotations".equals(m.getAttributes()[0].getName())) {
				int a0index = m.getAttributes()[0].getInfo()[2], a1index = m.getAttributes()[0].getInfo()[3];
				a0index = a0index < 0 ? a0index + 256 : a0index;
				a1index = a1index < 0 ? a1index + 256 : a1index;
				int aindex = (a0index << 8) | a1index;
				if(ASYNC_TYPE.equals(((ConstantUtf8)supercp[aindex]).getValue())) {
					String curClsName = taskClassName + (off + 1);
					overrideMethodNames[off] = m.getName();
					overrideMethodDescs[off] = m.getDesc();
					generateAsyncTask(curClsName, m.getName(), m.getDesc()).loadSelfClass();
					off++;
				}
			}
		}
		ClassBytecode newClassByteCode = generateAsyncClassCode(Arrays.copyOfRange(overrideMethodNames, 0, off), Arrays.copyOfRange(overrideMethodDescs, 0, off));
		return newClassByteCode.loadSelfClass();
	}

	public ClassBytecode generateAsyncClassCode(String overrideMethods[], String overrideMethodDescs[]) {
		if(overrideMethods.length != overrideMethodDescs.length) {
			throw new IllegalArgumentException("the number of overrideMethods and overrideMethodDescs must equal");
		}
		String simpleClassName = className.substring(className.lastIndexOf('/')+1);
		
		ClassBytecode newcls = new ClassBytecode();
		newcls.setClassName(className.replaceAll("/", "."));
		newcls.setMagic(superCls.getMagic());
		newcls.setMinorVersion(superCls.getMinorVersion());
		newcls.setMajorVersion(superCls.getMajorVersion());
		newcls.setAccessFlag(superCls.getAccessFlag());
		newcls.setClassIndex(1);
		newcls.setSuperIndex(3);
		newcls.setInterfaceCount(0);
		newcls.setInterfaceIndexArr(new int[0]);

		
		Map<String, Integer> nameIndexMap = new HashMap<>(128);
		
		int index = 1;
		ConstantInfo[] constants = new ConstantInfo[256];
		
		ConstantClass clazzInfo = new ConstantClass();
		clazzInfo.setNameIndex(2);
		constants[index++] = clazzInfo;
		
		ConstantUtf8 clazzUtf8Info = new ConstantUtf8();
		clazzUtf8Info.setValue(className);
		nameIndexMap.put(className, index);
		constants[index++] = clazzUtf8Info;
		
		int superClassIndex = index;
		ConstantClass superInfo = new ConstantClass();
		superInfo.setNameIndex(4);
		constants[index++] = superInfo;
		
		ConstantUtf8 superUtf8Info = new ConstantUtf8();
		superUtf8Info.setValue(superClassName);
		nameIndexMap.put(superClassName, index);
		constants[index++] = superUtf8Info;
		
		ConstantUtf8 poolUtf8 = new ConstantUtf8();
		poolUtf8.setValue(POOL_NAME);
		nameIndexMap.put(POOL_NAME, index);
		constants[index++] = poolUtf8;
		
		ConstantUtf8 poolClsUtf8 = new ConstantUtf8();
		poolClsUtf8.setValue(THREADPOOL_TYPE);
		nameIndexMap.put(THREADPOOL_TYPE, index);
		constants[index++] = poolClsUtf8;
		
		int poolIndex = index;
		ConstantMemberRef poolFieldRef = new ConstantMemberRef(ConstantInfo.CONSTANT_Fieldref);
		poolFieldRef.setClassIndex(1);
		poolFieldRef.setNameAndTypeIndex(index + 1);
		constants[index++] = poolFieldRef;
		
		ConstantNameAndType poolFieldNameType = new ConstantNameAndType();
		poolFieldNameType.setNameIndex(nameIndexMap.get(POOL_NAME));
		poolFieldNameType.setDescIndex(nameIndexMap.get(THREADPOOL_TYPE));
		constants[index++] = poolFieldNameType;

		ConstantUtf8 mcode = new ConstantUtf8();
		mcode.setValue("Code");
		nameIndexMap.put("Code", index);
		constants[index++] = mcode;

		ConstantUtf8 lnt = new ConstantUtf8();
		lnt.setValue("LineNumberTable");
		nameIndexMap.put("LineNumberTable", index);
		constants[index++] = lnt;

		ConstantUtf8 lvt = new ConstantUtf8();
		lvt.setValue("LocalVariableTable");
		nameIndexMap.put("LocalVariableTable", index);
		constants[index++] = lvt;
		

		ConstantUtf8 thi = new ConstantUtf8();
		thi.setValue(THIS_NAME);
		nameIndexMap.put(THIS_NAME, index);
		constants[index++] = thi;
		

		ConstantUtf8 thiType = new ConstantUtf8();
		thiType.setValue(classTypeName);
		nameIndexMap.put(classTypeName, index);
		constants[index++] = thiType;
		
		int consIndex = index;
		ConstantUtf8 mname = new ConstantUtf8();
		mname.setValue("<init>");
		nameIndexMap.put("<init>", index);
		constants[index++] = mname;
		
		MemberInfo[] methods = new MemberInfo[superMethods.length];
		int methodOff = 0;
		
		int lineIndex = 12;
		for(int i = 0; i < superMethods.length; i++) {
			MemberInfo m = superMethods[i];
			if("<init>".equals(m.getName())) {
				int descIndex = index;
				if(!nameIndexMap.containsKey(m.getDesc())) {
					ConstantUtf8 mdesc = new ConstantUtf8();
					mdesc.setValue(m.getDesc());
					nameIndexMap.put(m.getDesc(), index);
					constants[index++] = mdesc;
				} else {
					descIndex = nameIndexMap.get(m.getDesc());
				}
				
				int mrefIndex = index;
				ConstantMemberRef mref = new ConstantMemberRef(ConstantInfo.CONSTANT_Methodref);
				mref.setClassIndex(superClassIndex);
				mref.setNameAndTypeIndex( index + 1);
				constants[index++] = mref;

				ConstantNameAndType mnt = new ConstantNameAndType();
				mnt.setNameIndex(consIndex);
				mnt.setDescIndex(descIndex);
				constants[index++] = mnt;
				
				String[] args = parseMethodDesc(m.getDesc());
				int p = 0;
				for(String arg: args) {
					if(!nameIndexMap.containsKey(arg)) {
						ConstantUtf8 argType = new ConstantUtf8();
						argType.setValue(arg);
						nameIndexMap.put(arg, index);
						constants[index++] = argType;
					}
					String argName = "p" + p;
					if(!nameIndexMap.containsKey(argName)) {
						ConstantUtf8 argNameType = new ConstantUtf8();
						argNameType.setValue(argName);
						nameIndexMap.put(argName, index);
						constants[index++] = argNameType;
					}
					p++;
				}
				
				
			    /**
			     * constructor methods
			     * */
				int codeLength = 0;
				if(args.length > 3) {
					// alod_0: 1byte load 0~3: 3bytes, (load+index)*(args - 3): (args-3)*2 bytes, invoke: 3bytes, return 1byte
					codeLength = 1 + 3 + (args.length - 3) * 2 + 3 + 1;
				} else {
					// alod_0: 1byte load 0~3: 3bytes, invoke: 3bytes, return 1byte
					codeLength = 1 + args.length + 3 + 1;
				}
				byte[] cattCode = new byte[codeLength];
				cattCode[0] = InstructionTable.getInstructionCode("aload_0");
				int opIndex = 1;
				for(int j = 0; j < args.length; j++) {
					byte[] codes = InstructionTable.decodeLoadIns(args[j],  j);
					if(codes.length == 1) {
						cattCode[opIndex++] = codes[0];
					}
					if(codes.length == 2) {
						cattCode[opIndex++] = codes[0];
						cattCode[opIndex++] = codes[1];
					}
				}
				cattCode[opIndex++] = InstructionTable.getInstructionCode("invokespecial");
				cattCode[opIndex++] = (byte) ((mrefIndex >> 8) & 0xff);
				cattCode[opIndex++] = (byte) (mrefIndex & 0xff);
				cattCode[opIndex++] = InstructionTable.getInstructionCode("return");
				

				int[][] localVarArr = new int[args.length + 1][];
				localVarArr[0] = new int[] {0, codeLength, nameIndexMap.get(THIS_NAME), nameIndexMap.get(classTypeName), 0};
				for(int j = 1; j < localVarArr.length; j++) {
					localVarArr[j] = new int[] {0, codeLength, nameIndexMap.get("p" + (j-1)), nameIndexMap.get(args[j-1]), j};
				}
				
				int stacks = args.length + 1;
				MemberInfo init = generateMethod(nameIndexMap, classTypeName, m.getName(), m.getDesc(), args, stacks, stacks, cattCode, lineIndex, 1, 4, localVarArr);
				lineIndex += 4;
				methods[methodOff++] = init;
			}
		}
		/**
		 * submit here
		 * */
		int submitMethodIndex = index;
		ConstantMemberRef submitRef = new ConstantMemberRef(ConstantInfo.CONSTANT_Methodref);
		submitRef.setClassIndex(index+1);
		submitRef.setNameAndTypeIndex(index+3);
		constants[index++] = submitRef;

		ConstantClass submitClass = new ConstantClass();
		submitClass.setNameIndex(index + 1);
		constants[index++] = submitClass;

		ConstantUtf8 submitClassNameUtf8 = new ConstantUtf8();
		submitClassNameUtf8.setValue(THREADPOOL_CLASS);
		nameIndexMap.put(THREADPOOL_CLASS, index);
		constants[index++] = submitClassNameUtf8;
		
		ConstantNameAndType submitType = new ConstantNameAndType();
		submitType.setNameIndex(index + 1);
		submitType.setDescIndex(index + 2);
		constants[index++] = submitType;
		
		if(nameIndexMap.containsKey("submit")) {
			submitType.setNameIndex(nameIndexMap.get("submit"));
		} else {
			ConstantUtf8 submitName = new ConstantUtf8();
			submitName.setValue("submit");
			constants[index++] = submitName;
		}

		ConstantUtf8 submitDescName = new ConstantUtf8();
		submitDescName.setValue("("+SUBMIT_TASK_TYPE+")V");
		nameIndexMap.put(submitDescName.getValue(), index);
		constants[index++] = submitDescName;
		
		int[] innerClassCount = new int[16];
		int innerClassOff = 0;
		for(int i = 0; i < overrideMethods.length; i++) {

			String methodName = overrideMethods[i], methodDesc = overrideMethodDescs[i];
			String[] args = parseMethodDesc(methodDesc);
			
			/**
			 * task class here
			 * */
			int taskClassIndex = index;
			innerClassCount[innerClassOff++] = index;
			ConstantClass taskClass = new ConstantClass();
			taskClass.setNameIndex(index + 1);
			constants[index++] = taskClass;

			ConstantUtf8 taskClassNameUtf8 = new ConstantUtf8();
			taskClassNameUtf8.setValue(taskClassName + innerClassOff);
			nameIndexMap.put(taskClassNameUtf8.getValue(), index);
			constants[index++] = taskClassNameUtf8;

			int taskClassInitIndex = index;
			ConstantMemberRef taskInitRef = new ConstantMemberRef(ConstantInfo.CONSTANT_Methodref);
			taskInitRef.setClassIndex( index - 2);
			taskInitRef.setNameAndTypeIndex( index + 1);
			constants[index++] = taskInitRef;
			
			ConstantNameAndType taskType = new ConstantNameAndType();
			taskType.setNameIndex(consIndex);
			taskType.setDescIndex( index + 1);
			constants[index++] = taskType;
			
			
			String taskParamTypeName = "(L"+className+";";
			for(String arg: args) {
				taskParamTypeName += arg;
			}
			taskParamTypeName += ")V";
			if(nameIndexMap.containsKey(taskParamTypeName)) {
				taskType.setDescIndex( nameIndexMap.get(taskParamTypeName));
			} else {
				ConstantUtf8 taskParamName = new ConstantUtf8();
				taskParamName.setValue(taskParamTypeName);
				nameIndexMap.put(taskParamTypeName, index);
				constants[index++] = taskParamName;
			}
			
			String superOverName = generateSuperOverMethodName(methodName);
			if(!nameIndexMap.containsKey(superOverName)) {
				ConstantUtf8 mName = new ConstantUtf8();
				mName.setValue(superOverName);
				nameIndexMap.put(superOverName, index);
				constants[index++] = mName;
			}
			
			
			int mNameIndex = index;
			if(!nameIndexMap.containsKey(methodName)) {
				ConstantUtf8 mName = new ConstantUtf8();
				mName.setValue(methodName);
				nameIndexMap.put(methodName, index);
				constants[index++] = mName;
			} else {
				mNameIndex = nameIndexMap.get(methodName);
			}
			
			int mDescIndex = index;
			if(!nameIndexMap.containsKey(methodDesc)) {
				ConstantUtf8 mdesc = new ConstantUtf8();
				mdesc.setValue(methodDesc);
				nameIndexMap.put(methodDesc, index);
				constants[index++] = mdesc;
			} else {
				mDescIndex = nameIndexMap.get(methodDesc);
			}
			
			int superMethodRef = index;
			ConstantMemberRef mref = new ConstantMemberRef(ConstantInfo.CONSTANT_Methodref);
			mref.setClassIndex( superClassIndex);
			mref.setNameAndTypeIndex( index+1);
			constants[index++] = mref;

			ConstantNameAndType mnt = new ConstantNameAndType();
			mnt.setNameIndex(mNameIndex);
			mnt.setDescIndex(mDescIndex);
			constants[index++] = mnt;
			
			int p = 0;
			for(String arg: args) {
				if(!nameIndexMap.containsKey(arg)) {
					ConstantUtf8 argType = new ConstantUtf8();
					argType.setValue(arg);
					nameIndexMap.put(arg, index);
					constants[index++] = argType;
				}
				String argName = "p" + p;
				if(!nameIndexMap.containsKey(argName)) {
					ConstantUtf8 argNameType = new ConstantUtf8();
					argNameType.setValue(argName);
					nameIndexMap.put(argName, index);
					constants[index++] = argNameType;
				}
				p++;
			}
			
			
			
			/**
			 * async methods
			 * */
			{
				int codeLength = 0;
				if(args.length > 3) {
					// alod_0: 1byte load 0~3: 3bytes, (load+index)*(args - 3): (args-3)*2 bytes, invoke: 3bytes, return 1byte
					codeLength = 9 + 3 + (args.length - 3) * 2 + 7;
				} else {
					// alod_0: 1byte load 0~3: 3bytes, invoke: 3bytes, return 1byte
					codeLength = 9 + args.length + 7;
				}
				byte[] cattCode = new byte[codeLength];
				cattCode[0] = InstructionTable.getInstructionCode("aload_0");
				cattCode[1] = InstructionTable.getInstructionCode("getfield");
				cattCode[2] = (byte) ((poolIndex >> 8) & 0xff);
				cattCode[3] = (byte) (poolIndex & 0xff);
				cattCode[4] = InstructionTable.getInstructionCode("new");
				cattCode[5] = (byte) ((taskClassIndex >> 8) & 0xff);
				cattCode[6] = (byte) (taskClassIndex & 0xff);
				cattCode[7] = InstructionTable.getInstructionCode("dup");
				cattCode[8] = InstructionTable.getInstructionCode("aload_0");
				int opIndex = 9;
				for(int j = 0; j < args.length; j++) {
					byte[] codes = InstructionTable.decodeLoadIns(args[j],  j);
					if(codes.length == 1) {
						cattCode[opIndex++] = codes[0];
					}
					if(codes.length == 2) {
						cattCode[opIndex++] = codes[0];
						cattCode[opIndex++] = codes[1];
					}
				}
				
				cattCode[opIndex++] = InstructionTable.getInstructionCode("invokespecial");
				cattCode[opIndex++] = (byte) ((taskClassInitIndex >> 8) & 0xff);
				cattCode[opIndex++] = (byte) (taskClassInitIndex & 0xff);
				cattCode[opIndex++] = InstructionTable.getInstructionCode("invokevirtual");
				cattCode[opIndex++] = (byte) ((submitMethodIndex >> 8) & 0xff);
				cattCode[opIndex++] = (byte) (submitMethodIndex & 0xff);
				cattCode[opIndex++] = InstructionTable.getInstructionCode("return");
				
				int[][] localVarArr = new int[args.length + 1][];
				localVarArr[0] = new int[] {0, codeLength, nameIndexMap.get(THIS_NAME), nameIndexMap.get(classTypeName), 0};
				for(int j = 1; j < localVarArr.length; j++) {
					localVarArr[j] = new int[] {0, codeLength, nameIndexMap.get("p" + (j-1)), nameIndexMap.get(args[j-1]), j};
				}
				
				int stacks = args.length + 4, locals = args.length + 1;
				MemberInfo method = generateMethod(nameIndexMap, classTypeName, methodName, methodDesc, args, stacks, locals, cattCode, lineIndex, 5, 8, localVarArr);
				lineIndex += 8;
				methods[methodOff++] = method;
			}
			
			
			/**
			 * super methods
			 * */
			{
				int codeLength = 0;
				if(args.length > 3) {
					codeLength = 1 + 3 + (args.length - 3) * 2 + 4;
				} else {
					codeLength = 1 + args.length + 4;
				}
				byte[] cattCode = new byte[codeLength];
				cattCode[0] = InstructionTable.getInstructionCode("aload_0");
				int opIndex = 1;
				for(int j = 0; j < args.length; j++) {
					byte[] codes = InstructionTable.decodeLoadIns(args[j],  j);
					if(codes.length == 1) {
						cattCode[opIndex++] = codes[0];
					}
					if(codes.length == 2) {
						cattCode[opIndex++] = codes[0];
						cattCode[opIndex++] = codes[1];
					}
				}
				cattCode[opIndex++] = InstructionTable.getInstructionCode("invokespecial");
				cattCode[opIndex++] = (byte) ((superMethodRef >> 8) & 0xff);
				cattCode[opIndex++] = (byte) (superMethodRef & 0xff);
				cattCode[opIndex++] = InstructionTable.getInstructionCode("return");

				int[][] localVarArr = new int[args.length + 1][];
				localVarArr[0] = new int[] {0, codeLength, nameIndexMap.get(THIS_NAME), nameIndexMap.get(classTypeName), 0};
				for(int j = 1; j < localVarArr.length; j++) {
					localVarArr[j] = new int[] {0, codeLength, nameIndexMap.get("p" + (j-1)), nameIndexMap.get(args[j-1]), j};
				}
				
				int stacks = args.length + 1, locals = args.length + 1;
				MemberInfo method = generateMethod(nameIndexMap, classTypeName, superOverName, methodDesc, args, stacks, locals, cattCode, lineIndex, 1, 4, localVarArr);
				lineIndex += 4;
				methods[methodOff++] = method;
			}
		}
		newcls.setMethods(Arrays.copyOfRange(methods, 0, methodOff));
		newcls.setMethodCount(newcls.getMethods().length);
		
		MemberInfo[] fields = new MemberInfo[1];
		fields[0] = new MemberInfo();
		fields[0].setName(POOL_NAME);
		fields[0].setDesc(THREADPOOL_TYPE);
		fields[0].setAccessFlags(Modifier.PROTECTED);
		fields[0].setNameIndex(nameIndexMap.get(POOL_NAME));
		fields[0].setDescriptorIndex(nameIndexMap.get(THREADPOOL_TYPE));
		fields[0].setAttributesCount(0);
		fields[0].setAttributes(new AttributeInfo[0]);
		newcls.setFieldCount(1);
		newcls.setFields(fields);

		int sourceIndex = index;
		ConstantUtf8 source = new ConstantUtf8();
		source.setValue("SourceFile");
		constants[index++] = source;

		int sourceNameIndex = index;
		ConstantUtf8 sourceName = new ConstantUtf8();
		sourceName.setValue(simpleClassName+".java");
		constants[index++] = sourceName;

		int innerClassRawNameIndex = index;
		ConstantUtf8 innerClass = new ConstantUtf8();
		innerClass.setValue("InnerClasses");
		constants[index++] = innerClass;
		
		ConstantPool cp = new ConstantPool();
		cp.setCpInfo(Arrays.copyOfRange(constants, 0, index));
		cp.setConstantPoolCount(cp.getCpInfo().length);
		newcls.setConstantPool(cp);
		
		ClassEnd end = new ClassEnd();
		end.setSourceLen(2);
		end.setSourceIndex(sourceIndex);
		end.setSourceNop(2);
		end.setSourceNameIndex(sourceNameIndex);
		if(innerClassOff > 0) {
			end.setInnerClassRawNameIndex(innerClassRawNameIndex);
			end.setInnerClassLength(2 + innerClassOff * 8);
			end.setInnerClassCount(innerClassOff);
			int[][] innerClassTable = new int[innerClassOff][3];
			end.setInnerClassTable(innerClassTable);
			for(int i = 0; i < innerClassOff; i++) {
				innerClassTable[i][0] = innerClassCount[i];
				innerClassTable[i][1] = 0;
				innerClassTable[i][2] = 0;
			}
		}
		newcls.setClassEnd(end);
		
		return newcls;
	}
	
	private ClassBytecode generateAsyncTask(String curClsName, String methodName, String methodDesc) {

		String superOverMethodName = generateSuperOverMethodName(methodName);
		String simpleClassName = curClsName.substring(curClsName.lastIndexOf('/')+1);
		String[] args = parseMethodDesc(methodDesc);
		
		ClassBytecode newcls = new ClassBytecode();
		newcls.setClassName(curClsName.replaceAll("/", "."));
		newcls.setMagic(superCls.getMagic());
		newcls.setMinorVersion(superCls.getMinorVersion());
		newcls.setMajorVersion(superCls.getMajorVersion());
		newcls.setAccessFlag(superCls.getAccessFlag());
		newcls.setClassIndex(1);
		newcls.setSuperIndex(3);
		newcls.setInterfaceCount(1);
		newcls.setInterfaceIndexArr(new int[] {5});

		Map<String, Integer> nameIndexMap = new HashMap<>(128);
		Map<String, Integer> fieldIndexMap = new HashMap<>(128);
		Map<String, Integer> classIndexMap = new HashMap<>(128);
		
		int index = 1;
		ConstantInfo[] constants = new ConstantInfo[256];
		
		ConstantClass clazzInfo = new ConstantClass();
		clazzInfo.setNameIndex(2);
		classIndexMap.put(curClsName, index);
		constants[index++] = clazzInfo;
		
		ConstantUtf8 clazzUtf8Info = new ConstantUtf8();
		clazzUtf8Info.setValue(curClsName);
		nameIndexMap.put(curClsName, index);
		constants[index++] = clazzUtf8Info;

		ConstantClass objectInfo = new ConstantClass();
		objectInfo.setNameIndex(4);
		classIndexMap.put(OBJECT_CLASS, index);
		constants[index++] = objectInfo;
		
		ConstantUtf8 objectUtf8Info = new ConstantUtf8();
		objectUtf8Info.setValue(OBJECT_CLASS);
		nameIndexMap.put(OBJECT_CLASS, index);
		constants[index++] = objectUtf8Info;
		
		ConstantClass superInfo = new ConstantClass();
		superInfo.setNameIndex(6);
		classIndexMap.put(SUBMIT_TASK_CLASS, index);
		constants[index++] = superInfo;
		
		ConstantUtf8 superUtf8Info = new ConstantUtf8();
		superUtf8Info.setValue(SUBMIT_TASK_CLASS);
		nameIndexMap.put(SUBMIT_TASK_CLASS, index);
		constants[index++] = superUtf8Info;
		

		ConstantUtf8 curClassTypeName = new ConstantUtf8();
		curClassTypeName.setValue("L"+curClsName+";");
		nameIndexMap.put(curClassTypeName.getValue(), index);
		constants[index++] = curClassTypeName;
		
		ConstantUtf8 fieldUtf8 = new ConstantUtf8();
		fieldUtf8.setValue(TASK_FIELD_NAME);
		nameIndexMap.put(TASK_FIELD_NAME, index);
		constants[index++] = fieldUtf8;
		
		ConstantUtf8 fieldClsUtf8 = new ConstantUtf8();
		fieldClsUtf8.setValue(className);
		nameIndexMap.put(className, index);
		constants[index++] = fieldClsUtf8;

		ConstantUtf8 fieldClsTypeUtf8 = new ConstantUtf8();
		fieldClsTypeUtf8.setValue(classTypeName);
		nameIndexMap.put(classTypeName, index);
		constants[index++] = fieldClsTypeUtf8;
		
		ConstantMemberRef fieldRef = new ConstantMemberRef(ConstantInfo.CONSTANT_Fieldref);
		fieldRef.setClassIndex( 1);
		fieldRef.setNameAndTypeIndex( index + 1);
		fieldIndexMap.put(TASK_FIELD_NAME, index);
		constants[index++] = fieldRef;
		
		ConstantNameAndType fieldNameType = new ConstantNameAndType();
		fieldNameType.setNameIndex(nameIndexMap.get(TASK_FIELD_NAME));
		fieldNameType.setDescIndex(nameIndexMap.get(classTypeName));
		constants[index++] = fieldNameType;
		
		int p = 0;
		for(String arg: args) {
			if(!nameIndexMap.containsKey(arg)) {
				ConstantUtf8 argType = new ConstantUtf8();
				argType.setValue(arg);
				nameIndexMap.put(arg, index);
				constants[index++] = argType;
			}
			String argName = "p" + p;
			if(!nameIndexMap.containsKey(argName)) {
				ConstantUtf8 argNameType = new ConstantUtf8();
				argNameType.setValue(argName);
				nameIndexMap.put(argName, index);
				constants[index++] = argNameType;
			}

			ConstantMemberRef fRef = new ConstantMemberRef(ConstantInfo.CONSTANT_Fieldref);
			fRef.setClassIndex( 1);
			fRef.setNameAndTypeIndex( index + 1);
			fieldIndexMap.put(argName, index);
			constants[index++] = fRef;
			
			ConstantNameAndType fNameType = new ConstantNameAndType();
			fNameType.setNameIndex(nameIndexMap.get(argName));
			fNameType.setDescIndex( nameIndexMap.get(arg));
			constants[index++] = fNameType;
			p++;
		}
		
		//define $ArcherTask1.<init>(args) utf8
		ConstantUtf8 consName = new ConstantUtf8();
		consName.setValue("<init>");
		nameIndexMap.put("<init>", index);
		constants[index++] = consName;

		String consDescStr = "(" + classTypeName;
		for(String arg: args) {
			consDescStr += arg;
		}
		consDescStr += ")V";
		ConstantUtf8 consDesc = new ConstantUtf8();
		consDesc.setValue(consDescStr);
		nameIndexMap.put(consDescStr, index);
		constants[index++] = consDesc;

		ConstantUtf8 runDesc = new ConstantUtf8();
		runDesc.setValue("()V");
		nameIndexMap.put("()V", index);
		constants[index++] = runDesc;

		int callSuperIndex = index;
		ConstantMemberRef callSuperRef = new ConstantMemberRef(ConstantInfo.CONSTANT_Methodref);
		callSuperRef.setClassIndex( 3);
		callSuperRef.setNameAndTypeIndex( index + 1);
		constants[index++] = callSuperRef;

		ConstantNameAndType superCallnt = new ConstantNameAndType();
		superCallnt.setNameIndex(nameIndexMap.get("<init>"));
		superCallnt.setDescIndex( nameIndexMap.get("()V"));
		constants[index++] = superCallnt;

		ConstantUtf8 insCallName = new ConstantUtf8();
		insCallName.setValue(methodName);
		nameIndexMap.put(methodName, index);
		constants[index++] = insCallName;
		
		// define $impl.super$call(args)
		ConstantClass insCallClass = new ConstantClass();
		insCallClass.setNameIndex(nameIndexMap.get(className));
		classIndexMap.put(className, index);
		constants[index++] = insCallClass;
		
		int insCallmIndex = index;
		ConstantMemberRef insCallRef = new ConstantMemberRef(ConstantInfo.CONSTANT_Methodref);
		insCallRef.setClassIndex( index - 1);
		insCallRef.setNameAndTypeIndex( index + 1);
		constants[index++] = insCallRef;

		ConstantNameAndType insCallnt = new ConstantNameAndType();
		insCallnt.setNameIndex(index + 1);
		insCallnt.setDescIndex( index + 2);
		constants[index++] = insCallnt;

		ConstantUtf8 insSuperCallName = new ConstantUtf8();
		insSuperCallName.setValue(superOverMethodName);
		nameIndexMap.put(superOverMethodName, index);
		constants[index++] = insSuperCallName;

		ConstantUtf8 insCallDesc = new ConstantUtf8();
		insCallDesc.setValue(methodDesc);
		nameIndexMap.put(methodDesc, index);
		constants[index++] = insCallDesc;
		
		//define run()
		ConstantUtf8 runName = new ConstantUtf8();
		runName.setValue("run");
		nameIndexMap.put("run", index);
		constants[index++] = runName;
		
		
		ConstantUtf8 mcode = new ConstantUtf8();
		mcode.setValue("Code");
		nameIndexMap.put("Code", index);
		constants[index++] = mcode;

		ConstantUtf8 lnt = new ConstantUtf8();
		lnt.setValue("LineNumberTable");
		nameIndexMap.put("LineNumberTable", index);
		constants[index++] = lnt;

		ConstantUtf8 lvt = new ConstantUtf8();
		lvt.setValue("LocalVariableTable");
		nameIndexMap.put("LocalVariableTable", index);
		constants[index++] = lvt;
		

		ConstantUtf8 thi = new ConstantUtf8();
		thi.setValue(THIS_NAME);
		nameIndexMap.put(THIS_NAME, index);
		constants[index++] = thi;
		

		ConstantUtf8 thiType = new ConstantUtf8();
		thiType.setValue("L"+curClsName+";");
		nameIndexMap.put(thiType.getValue(), index);
		constants[index++] = thiType;
		
		
		MemberInfo[] methods = new MemberInfo[2];
		newcls.setMethodCount(2);
		newcls.setMethods(methods);
		int lineIndex = 12 + args.length * 2;
		
		/**
	     * constructor methods
	     * */
		{

			int codeLength = 0;
			if(args.length > 2) {
				// alod_0: 1byte load 0~3: 3bytes, (load+index)*(args - 3): (args-3)*2 bytes, invoke: 3bytes, return 1byte
				codeLength = 5 + 2 * 3 + (args.length - 2) * (5 + 1) + 5;
			} else {
				// alod_0: 1byte load 0~3: 3bytes, invoke: 3bytes, return 1byte
				codeLength = 5 + args.length * 5 + 5;
			}
			byte[] cattCode = new byte[codeLength];
			int opIndex = 0;
			cattCode[opIndex++] = InstructionTable.getInstructionCode("aload_0");
			cattCode[opIndex++] = InstructionTable.getInstructionCode("aload_1");
			cattCode[opIndex++] = InstructionTable.getInstructionCode("putfield");
			int fieldIndex = fieldIndexMap.get(TASK_FIELD_NAME);
			cattCode[opIndex++] = (byte) ((fieldIndex >> 8) & 0xff);
			cattCode[opIndex++] = (byte) (fieldIndex & 0xff);
			for(int i = 0; i < args.length; i++) {
				cattCode[opIndex++] = InstructionTable.getInstructionCode("aload_0");
				byte[] codes = InstructionTable.decodeLoadIns(args[i],  i + 1);
				if(codes.length == 1) {
					cattCode[opIndex++] = codes[0];
				}
				if(codes.length == 2) {
					cattCode[opIndex++] = codes[0];
					cattCode[opIndex++] = codes[1];
				}
				cattCode[opIndex++] = InstructionTable.getInstructionCode("putfield");
				int fIndex = fieldIndexMap.get("p"+i);
				cattCode[opIndex++] = (byte) ((fIndex >> 8) & 0xff);
				cattCode[opIndex++] = (byte) (fIndex & 0xff);
				
			}
			cattCode[opIndex++] = InstructionTable.getInstructionCode("aload_0");
			cattCode[opIndex++] = InstructionTable.getInstructionCode("invokespecial");
			cattCode[opIndex++] = (byte) ((callSuperIndex >> 8) & 0xff);
			cattCode[opIndex++] = (byte) (callSuperIndex & 0xff);
			cattCode[opIndex++] = InstructionTable.getInstructionCode("return");

			int[][] localVarArr = {{0, codeLength, nameIndexMap.get(THIS_NAME), nameIndexMap.get("L"+curClsName+";"), 0}};
			
			int stacks = 2, locals = args.length + 2;
			methods[0] = generateMethod(nameIndexMap, "L"+curClsName+";", "<init>", consDescStr , args, stacks, locals, cattCode, lineIndex, args.length + 1, args.length + 3, localVarArr);
			lineIndex += args.length + 3;
		}
		
		/**
		 * run
		 * */
		{

			int codeLength = 4 + args.length * 4 + 4;
			byte[] cattCode = new byte[codeLength];
			int opIndex = 0;
			cattCode[opIndex++] = InstructionTable.getInstructionCode("aload_0");
			cattCode[opIndex++] = InstructionTable.getInstructionCode("getfield");
			int fieldIndex = fieldIndexMap.get(TASK_FIELD_NAME);
			cattCode[opIndex++] = (byte) ((fieldIndex >> 8) & 0xff);
			cattCode[opIndex++] = (byte) (fieldIndex & 0xff);
			
			for(int i = 0; i < args.length; i++) {
				cattCode[opIndex++] = InstructionTable.getInstructionCode("aload_0");
				cattCode[opIndex++] = InstructionTable.getInstructionCode("getfield");
				int fIndex = fieldIndexMap.get("p"+i);
				cattCode[opIndex++] = (byte) ((fIndex >> 8) & 0xff);
				cattCode[opIndex++] = (byte) (fIndex & 0xff);
			}
			cattCode[opIndex++] = InstructionTable.getInstructionCode("invokevirtual");
			cattCode[opIndex++] = (byte) ((insCallmIndex >> 8) & 0xff);
			cattCode[opIndex++] = (byte) (insCallmIndex & 0xff);
			cattCode[opIndex++] = InstructionTable.getInstructionCode("return");

			int[][] localVarArr = {{0, codeLength, nameIndexMap.get(THIS_NAME), nameIndexMap.get("L"+curClsName+";"), 0}};
			
			int stacks = 2, locals = 1;
			methods[1] = generateMethod(nameIndexMap, "L"+curClsName+";", "run", "()V" , args, stacks, locals, cattCode, lineIndex, args.length + 1, args.length + 3, localVarArr);
		}
		

		MemberInfo[] fields = new MemberInfo[args.length + 1];
		newcls.setFieldCount(args.length + 1);
		newcls.setFields(fields);
		
		fields[0] = new MemberInfo();
		fields[0].setName(TASK_FIELD_NAME);
		fields[0].setDesc(className);
		fields[0].setAccessFlags(Modifier.PROTECTED);
		fields[0].setNameIndex(nameIndexMap.get(TASK_FIELD_NAME));
		fields[0].setDescriptorIndex(nameIndexMap.get(classTypeName));
		fields[0].setAttributesCount(0);
		fields[0].setAttributes(new AttributeInfo[0]);
		for(int i = 0; i < args.length; i++) {
			String name = "p" + i;
			fields[i + 1] = new MemberInfo();
			fields[i + 1].setName(name);
			fields[i + 1].setName(args[i]);
			fields[i + 1].setAccessFlags(Modifier.PROTECTED);
			fields[i + 1].setNameIndex(nameIndexMap.get(name));
			fields[i + 1].setDescriptorIndex(nameIndexMap.get(args[i]));
			fields[i + 1].setAttributesCount(0);
			fields[i + 1].setAttributes(new AttributeInfo[0]);
		}
		
		int sourceIndex = index;
		ConstantUtf8 source = new ConstantUtf8();
		source.setValue("SourceFile");
		constants[index++] = source;

		int sourceNameIndex = index;
		ConstantUtf8 sourceName = new ConstantUtf8();
		sourceName.setValue(simpleClassName+".java");
		constants[index++] = sourceName;

		ConstantUtf8 enclosingMethodName = new ConstantUtf8();
		enclosingMethodName.setValue("EnclosingMethod");
		constants[index++] = enclosingMethodName;

		int innerClassRawNameIndex = index;
		ConstantUtf8 innerClass = new ConstantUtf8();
		innerClass.setValue("InnerClasses");
		constants[index++] = innerClass;
		
		ConstantPool cp = new ConstantPool();
		cp.setCpInfo(Arrays.copyOfRange(constants, 0, index));
		cp.setConstantPoolCount(cp.getCpInfo().length);
		newcls.setConstantPool(cp);
		
		ClassEnd end = new ClassEnd();
		end.setSourceLen(2);
		end.setSourceIndex(sourceIndex);
		end.setSourceNop(2);
		end.setSourceNameIndex(sourceNameIndex);
		end.setInnerClassRawNameIndex(innerClassRawNameIndex);
		end.setInnerClassLength(10);
		end.setInnerClassCount(1);
		int[][] innerClassTable = new int[1][3];
		end.setInnerClassTable(innerClassTable);
		innerClassTable[0][0] = 1;
		innerClassTable[0][1] = 0;
		innerClassTable[0][2] = 0;
		newcls.setClassEnd(end);
		
		
		return newcls;
	}
	
	private MemberInfo generateMethod(Map<String, Integer> nameIndexMap, String curClassType, String name, String desc, String args[], 
			int stacks,  int locals, byte[] cattCode, int lineIndex, int lineLen, int gap, int[][] localVarArr) {

		MemberInfo method = new MemberInfo();
		method.setName(name);
		method.setDesc(desc);
		method.setAccessFlags(Modifier.PUBLIC);
		method.setNameIndex(nameIndexMap.get(name));
		method.setDescriptorIndex(nameIndexMap.get(desc));
		method.setAttributesCount(1);
		AttributeInfo[] mattr = new AttributeInfo[1];
		method.setAttributes(mattr);

		CodeAttribute catt = new CodeAttribute();
		mattr[0] = catt;
		catt.setName("Code");
		catt.setNameIndex(nameIndexMap.get("Code"));
		catt.setMaxStack(stacks);
		catt.setMaxLocals(locals);
		catt.setCodeLength(cattCode.length);
		catt.setCode(cattCode);
		catt.setExcepetionTableLength(0);
		catt.setException(new byte[] {});
		catt.setAttributesCount(2);
		catt.setLength(20 + catt.getCodeLength());
		AttributeInfo[] cattAttr = new AttributeInfo[2];
		catt.setAttributes(cattAttr);
		LineNumAttribute lna = new LineNumAttribute();
		cattAttr[0] = lna;
		lna.setName("LineNumberTable");
		lna.setNameIndex(nameIndexMap.get("LineNumberTable"));
		lna.setLength(10);
		catt.setLength(catt.getLength() + lna.getLength() + 2);
		lna.setLineNumTableCount(2);
		lna.setLineNumTable(new int[][] {{0, lineIndex}, {catt.getCodeLength()-1, lineIndex+lineLen}});
		lineIndex += gap;
		LocalVarAttribute lva = new LocalVarAttribute();
		cattAttr[1] = lva;
		lva.setName("LocalVariableTable");
		lva.setNameIndex(nameIndexMap.get("LocalVariableTable"));
		lva.setLocalVarTableCount(localVarArr.length);
		lva.setLength(2 + lva.getLocalVarTableCount() * 10);
		catt.setLength(catt.getLength() + lva.getLength() + 2);
		lva.setLocalVarTable(localVarArr);
		
		return method;
	}
	
	
	private boolean checkPrimitive(byte c) {
		return c == 'Z' || c == 'B' || c == 'C' || c == 'S' || c == 'I' || c == 'J' || c == 'F' || c == 'D';
	}
	
	private String[] parseMethodDesc(String desc) {
		byte[] bs = desc.getBytes();
		String[] types = new String[16];
		int off = 0, typeS = 0, started = 0;
		for(int i = 1; i < bs.length - 1; i++) {
			if(bs[i] == ')') {
				break ;
			}
			if(started == 0 && checkPrimitive(bs[i])) {
				types[off++] = String.valueOf((char)bs[i]);
				continue ;
			}
			if(started == 0 && bs[i] == '[') {
				boolean ok = false;
				typeS = i;
				i++;
				for(;i < bs.length - 1; i++) {
					if(checkPrimitive(bs[i])) {
						types[off++] = new String(Arrays.copyOfRange(bs, typeS, i+1));
						ok = true;
					}
				}
				if(!ok) {
					throw new IllegalArgumentException("invalid method descriptor " + desc);
				}
				continue ;
			}
			if(bs[i] == 'L') {
				started = 1;
				typeS = i;
				continue ;
			}
			if(started == 1 && bs[i] == ';') {
				started = 0;
				types[off++] = new String(Arrays.copyOfRange(bs, typeS, i+1));
				continue;
			}
		}
		return Arrays.copyOfRange(types, 0, off);
	}
	
	private static String generateSuperOverMethodName(String name) {
		return "super$"+name;
	}
	
}

