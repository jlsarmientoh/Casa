package co.edu.uniandes.smarthome.client;
import java.io.File;
import java.io.FileInputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import co.edu.uniandes.ecriptador.Cifrador;

public class PoolDisparadorTramas implements Executor {
	
	public static String PUBLIC_KEY;
	public static String HOST;
	public static String SECONDARY_HOST;
	public static int PRIMARY_PORT;
	public static int SECONDARY_PORT;

	@Override
	public void execute(Runnable runnable) {
		// TODO Auto-generated method stub
		new Thread(runnable).start();
	}

	public void execute(Runnable runnable, int times) {
		for (int i = 0; i < times; i++) {
			execute(runnable);
		}
	}

	public static void main(String[] args) {
		/*Runnable runnable = new Task();
		Thread thread = new Thread(runnable);
		thread.start();
		PoolDisparadorTramas executor = new PoolDisparadorTramas();
		executor.execute(runnable, 100);*/
		FileInputStream fis;
		try{
	    	fis = new FileInputStream( new File( "./config/manager.properties" ) );
			Properties properties = new Properties( );
	        properties.load( fis );
	        PUBLIC_KEY = properties.getProperty("security.publickey");
	        HOST = properties.getProperty("host");
	        SECONDARY_HOST = properties.getProperty("secondaryHost");
	        PRIMARY_PORT = Integer.parseInt(properties.getProperty("primaryPort"));
	        SECONDARY_PORT = Integer.parseInt(properties.getProperty("secondaryPort"));
    	}catch(Exception e){
    		e.printStackTrace();
    		System.out.println("No se cargaron las propiedades, se usa el puerto por defecto");
    	}
		try{
			
			if(args != null && args.length == 1){
				Runnable runnable = new Task();
				int minutos=5;
				int numeroTramas= Integer.parseInt(args[0]); // Tramas a enviar
				/*Thread thread = new Thread(runnable);
				thread.start();*/
				PoolDisparadorTramas executor = new PoolDisparadorTramas();
				System.out.println("Empezamos el proceso con " + numeroTramas);
				
				while(true)
				{
					for(int i=0; i<minutos; i++)
					{
						for(int j=0; j<60; j++)
						{
						    //executor = new PoolDisparadorTramas();
							executor.execute(runnable, numeroTramas);
							delay();
							System.out.println(i+":"+j);
						}
						System.out.println("minuto " + i );
					}
					//minutos=minutos+60;
					//numeroTramas = numeroTramas + (numeroTramas*1/2);
					System.out.println("Numero de tramas " + numeroTramas);
				}
			}else{
				System.out.println("Debe indicar los parámetros tramas a enviar\n\n\r"
						+ "Ejemplo:\n\r"
						+ "java -jar casa.jar 10");
			}
		
		}catch(NumberFormatException nfe){
			System.out.println("Debe indicar los parámetros tramas a enviar\n\n\r"
					+ "Ejemplo:\n\r"
					+ "java -jar casa.jar 10");
			
		}catch(IndexOutOfBoundsException iob){
			System.out.println("Debe indicar los parámetros tramas a enviar\n\n\r"
					+ "Ejemplo:\n\r"
					+ "java -jar casa.jar 10");
		}
	}
	
	private static void delay(){
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException e)
		{
			
		}
	}

}

class Task implements Runnable {

	public void run() {

		try {
			send(PoolDisparadorTramas.HOST, PoolDisparadorTramas.PRIMARY_PORT);
		}
		catch (Exception e) {
			e.printStackTrace();
			try{
				send(PoolDisparadorTramas.SECONDARY_HOST, PoolDisparadorTramas.SECONDARY_PORT);
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	public void send(String host, int port) throws Exception{
		
		InetSocketAddress hostAddress = new InetSocketAddress(host,
				port);
		AsynchronousSocketChannel clientSocketChannel = AsynchronousSocketChannel
				.open();
		Future<Void> connectFuture = clientSocketChannel
				.connect(hostAddress);
		connectFuture.get(); // Wait until connection is done.
		// Genero un random para que sera el identificador de la casa

		long timeStamp = System.currentTimeMillis();
		int idCasa = (int) (Math.floor(Math.random() * (100 - 1 + 1) + 1));
		//System.out.println("Time stamp(" + timeStamp + ") idCasa(" + idCasa
		//		+ ")");
		//byte[] newData = new byte[20];
		byte[] newData = new byte[22];

		newData[0] = (byte) timeStamp;
		newData[1] = (byte) (timeStamp >> 8);
		newData[2] = (byte) (timeStamp >> 16);
		newData[3] = (byte) (timeStamp >> 24);
		newData[4] = (byte) (timeStamp >> 32);
		newData[5] = (byte) (timeStamp >> 40);
		newData[6] = (byte) (timeStamp >> 48);
		newData[7] = (byte) (timeStamp >> 56);
		newData[8] = (byte) idCasa;
		newData[9] = (byte) (idCasa >> 8);
		
		for (int i = 0; i < (40 / 4); i += 2) {
			// 4 Sensores por byte: Donde el nibble bajo representa el bit
			// de cambio y el nibble alto el estado
			newData[i + 10] = (byte) 0x0C;
		}
		
		/*
		 * Implementacion CheckSum CRC16
		 */
		int crc = CRC16.crc16(newData);
		System.out.println("crc16 " + crc);
		//Para incluir el crc
		newData[20] = (byte) crc;
		newData[21] = (byte) (crc >> 8);


		// se construye la trama con el random generado un separador y los
		// 50 sensores
		// el separados nos va servir para partir la cadena desde el
		// balanceador
		// String newData = idCasa
		// + "-1111111111111111111111111111111111111111111111111";
		Cifrador cifrador = new Cifrador(null, PoolDisparadorTramas.PUBLIC_KEY);
		
		byte[] ciphredData = cifrador.encrypt(newData);
		ByteBuffer buf = ByteBuffer.allocate(256);
		buf.clear();
		//buf.put(newData);
		System.out.println("Trama base:{"+newData+"}Trama ofuscada{"+ciphredData+"}");
		buf.put(ciphredData);
		buf.flip();
		clientSocketChannel.write(buf);
		clientSocketChannel.close();
	}
	
}
