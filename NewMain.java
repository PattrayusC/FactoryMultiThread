//Thanatorn Ruangrote 6313129
//Thunyavut Nabhaboriraks 6313130
//Pattrayus Chokbunlue 6313179

import java.io.*;
import java.util.*;

class OneShareMaterial {
    private String name;
    private int balance;
    
    public OneShareMaterial(String name) {
        this.name = name;
        balance = 0;
    }
    
    synchronized public void put(int balance) {
        this.balance += balance;
        System.out.printf("Thread %s\t>> Put %d %s\tbalance = %d %s\r\n", Thread.currentThread().getName(), balance, this.name, this.balance, this.name);
    }
    
    synchronized public int get(int balance) {
        if (this.balance < balance) {
            balance = this.balance;
        }
        this.balance -= balance;
        System.out.printf("Thread %s\t>> Get %d %s\tbalance = %d %s\r\n", Thread.currentThread().getName(), balance, this.name, this.balance, this.name);
        return balance;
    }
    
    public String getname() { return name; }
}

class Factory extends Thread implements Comparable<Factory>{
    private int ID;
    private String product;
    private int lotSize;
    private ArrayList<Integer> materials;
    
    private ArrayList<OneShareMaterial> list;
    private int[] stocks;
    private int count;
    private int period;
    
    public Factory(int ID, String product, int lotSize, ArrayList<Integer> materials, ArrayList<OneShareMaterial> list) {
        super(product);
        this.ID = ID;
        this.product = product;
        this.lotSize = lotSize;
        this.materials = materials;
        
        this.list = list;
        stocks = new int[list.size()];
        count = 1;
    }
    
    public int compareTo(Factory other) {
        int compare = other.getcount();
        return this.getcount()-compare;
    }
    
    public void setperiod(int period) { this.period = period; }
    public String getname() { return product; }
    public int getcount() { return count; }
    
    public void run() {
        for (int i=0; i<period; i++) {
            synchronized (list) {
                try { list.wait(); } catch(InterruptedException e) { }
            }
            boolean success = true;
            for (int j=0; j<list.size(); j++) {
                int require = (lotSize * materials.get(j)) - stocks[j];
                if ( stocks[j] < lotSize * materials.get(j) ) {
                    int stock = list.get(j).get(require);
                    stocks[j] += stock;
                    if (stocks[j] < lotSize * materials.get(j)) {
                        success = false;
                    }
                }
            }
            if (success == true) {
                for (int j=0; j<list.size(); j++) {
                    stocks[j] -= lotSize * materials.get(j);
                }
                System.out.printf("Thread %s\t>> +++++ Complete Lot %d\r\n", Thread.currentThread().getName(), count);
                count++;
            }
            else {
                System.out.printf("Thread %s\t>> ----- Fail\r\n", Thread.currentThread().getName());
             }
        }
    }
}

public class NewMain {
    public static void main(String[] args) {
        Scanner keyboardScan = new Scanner(System.in);
        System.out.printf("Thread %s\t>> Enter product specification file =\r\n", Thread.currentThread().getName());
        String infile = keyboardScan.next();
        
        boolean opensuccess = false;
        while (!opensuccess) {
            try (Scanner scans = new Scanner(new File(infile));) {
                opensuccess = true;
                boolean firstline = true;
                ArrayList<OneShareMaterial> list = new ArrayList<>();
                ArrayList<Factory> threads = new ArrayList<>();

                while (scans.hasNext()) {
                    String line = scans.nextLine();
                    String[] buf = line.split(",");

                    if (firstline == true) {
                        for (int i=0; i<buf.length; i++) {
                            OneShareMaterial item = new OneShareMaterial(buf[i].trim());
                            list.add(item);
                        }
                    }
                    else {
                        int ID = Integer.parseInt(buf[0].trim());
                        String name = buf[1].trim();
                        int lotSize = Integer.parseInt(buf[2].trim());
                        ArrayList<Integer> materials = new ArrayList<>();

                        System.out.printf("\r\nThread %s\t>> %-7s factory\t%3d units per lot\tmaterials per lot = ", Thread.currentThread().getName(), name, lotSize);
                        for (int i=3; i<buf.length; i++) {
                            int perunit = Integer.parseInt(buf[i].trim());
                            materials.add(perunit);
                            if (i == buf.length-1) {
                                System.out.printf("%3d %s", perunit * lotSize, list.get(i-3).getname());
                            }
                            else {
                                System.out.printf("%3d %s, ", perunit * lotSize, list.get(i-3).getname());
                            }
                        }

                        Factory thread = new Factory(ID, name, lotSize, materials, list);
                        threads.add(thread);
                    }

                    firstline = false;
                }
                
                int perday = 0, period = 0;
                
                System.out.println("\r\n");
                boolean correctinput = false;
                while (!correctinput) {
                    try {
                        Scanner keyboardScans = new Scanner(System.in);
                        System.out.printf("Thread %s\t>> Enter amount of material per day =\r\n", Thread.currentThread().getName());
                        perday = keyboardScans.nextInt();
                        correctinput = true;
                    }
                    catch (Exception e) { System.out.println(e); }
                }
                
                System.out.println("\r\n");
                correctinput = false;
                while (!correctinput) {
                    try {
                        Scanner keyboardScans = new Scanner(System.in);
                        System.out.printf("Thread %s\t>> Enter number of days =\r\n", Thread.currentThread().getName());
                        period = keyboardScans.nextInt();
                        correctinput = true;
                    }
                    catch (Exception e) { System.out.println(e); }
                }
                
                for (Factory obj : threads) {
                    obj.setperiod(period);
                    obj.start();
                }
                
                for (int i=0; i<period; i++) {
                    int count = 0;
                    while (count != threads.size()) {
                        count = 0;
                        for (Factory obj : threads) {
                            if ( obj.getState() == Thread.State.WAITING || obj.getState() == Thread.State.TERMINATED ) {
                                count++;
                            }
                        }
                    }
                    System.out.printf("\r\nThread %s\t>> Day %d\r\n", Thread.currentThread().getName(), i+1);
                    for (OneShareMaterial obj : list) {
                        obj.put(perday);
                    }
                    System.out.println();
                    synchronized (list) {
                        list.notifyAll();
                    }
                }
                
                for (Factory obj : threads) {
                    try { obj.join(); } catch (InterruptedException e) { }
                }
                
                Collections.sort(threads);
                System.out.printf("\r\nThread %s\t>> Summary\r\n", Thread.currentThread().getName());
                for (Factory obj : threads) {
                    System.out.printf("Thread %s\t>> Total %-7s Lots = %3d\r\n", Thread.currentThread().getName(), obj.getname(), obj.getcount());
                }
            }
            catch (FileNotFoundException e) {
                System.out.println(e);
                System.out.println("New file name = ");
                infile = keyboardScan.next();
            }
        }
    }
}