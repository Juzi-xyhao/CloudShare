package com.CloudShare.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Test {
    public void IOMethod(String TPATH){
        long start = System.currentTimeMillis();
        try {
            DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(new File(TPATH))));
            for(int i=0;i<4000000;i++){
                dos.writeInt(i);//写入 4000000 个整数
            }
            if(dos!=null){
                dos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        long time;
        time = end - start;
        System.out.println("IO方法写入耗时:" + time + "ms");
        start = System.currentTimeMillis();
        try {
            DataInputStream dis = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(new File(TPATH))));
            for(int i=0;i<4000000;i++){
                dis.readInt();
            }
            if(dis!=null){
                dis.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        end = System.currentTimeMillis();
        time = end - start;
        System.out.println("IO方法读取耗时:" + time + "ms");
    }

    public void ByteMethod(String TPATH){
        long start = System.currentTimeMillis();
        try {
            FileOutputStream fout = new FileOutputStream(new File(TPATH));
            FileChannel fc = fout.getChannel();//得到文件通道
            ByteBuffer byteBuffer = ByteBuffer.allocate(4000000*4);//分配 Buffer
            for(int i=0;i<4000000;i++){
                byteBuffer.put(int2byte(i));//将整数转为数组
            }
            byteBuffer.flip();//准备写
            fc.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        long time = end - start;
        System.out.println("Byte方法写入耗时:" + time + "ms");

        start = System.currentTimeMillis();
        FileInputStream fin;
        try {
            fin = new FileInputStream(new File(TPATH));
            FileChannel fc = fin.getChannel();//取得文件通道
            ByteBuffer byteBuffer = ByteBuffer.allocate(4000000*4);//分配 Buffer
            fc.read(byteBuffer);//读取文件数据
            fc.close();
            byteBuffer.flip();//准备读取数据
            while(byteBuffer.hasRemaining()){
                byte2int(byteBuffer.get(),byteBuffer.get(),byteBuffer.get(),byteBuffer.get());//将 byte 转为整数
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        end = System.currentTimeMillis();
        time = end - start;
        System.out.println("Byte方法读取耗时:" + time + "ms");
    }

    public void mapMethod(String TPATH){
        long start = System.currentTimeMillis();
        //将文件直接映射到内存的方法
        try {
            FileChannel fc = new RandomAccessFile(TPATH,"rw").getChannel();
            IntBuffer ib = fc.map(FileChannel.MapMode.READ_WRITE, 0, 4000000*4).asIntBuffer();
            for(int i=0;i<4000000;i++){
                ib.put(i);
            }
            if(fc!=null){
                fc.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        long time = end - start;
        System.out.println("map方法写入耗时:" + time + "ms");

        start =System.currentTimeMillis();
        try {
            FileChannel fc = new FileInputStream(TPATH).getChannel();
            MappedByteBuffer lib = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            lib.asIntBuffer();
            while(lib.hasRemaining()){
                lib.get();
            }
            if(fc!=null){
                fc.close();
            }} catch (IOException e) {
            e.printStackTrace();
        }
        end = System.currentTimeMillis();
        time = end - start;
        System.out.println("map方法读取耗时:" + time + "ms");


    }public static byte[] int2byte(int res){
        byte[] targets = new byte[4];
        targets[3] = (byte)(res & 0xff);//最低位
        targets[2] = (byte)((res>>8)&0xff);//次低位
        targets[1] = (byte)((res>>16)&0xff);//次高位
        targets[0] = (byte)((res>>>24));//最高位，无符号右移
        return targets;
    }public static int byte2int(byte b1,byte b2,byte b3,byte b4){
        return ((b1 & 0xff)<<24)|((b2 & 0xff)<<16)|((b3 & 0xff)<<8)|(b4 & 0xff);
    }

    public static void main(String[] args){
        Test nio = new Test();
        nio.IOMethod("d://1.txt");
        nio.ByteMethod("d://2.txt");
        nio.ByteMethod("d://3.txt");
    }
}

