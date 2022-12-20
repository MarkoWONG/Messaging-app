# Socket Programming Messenging Application

## File Names & Execution
The main code for the server and client should be contained in the following files: Client.java or client.py. 
The server accept the following three arguments:
- server_port: this is the port number which the server will use to communicate with the clients. Recall that a TCP socket is NOT uniquely identified by the server port number. So, it is possible for multiple TCP connections to use the same server-side port number.
- block_duration: this is the duration in seconds for which a user should be blocked after three unsuccessful authentication attempts.
- timeout: this is the duration in seconds of inactivity after which a user is logged off by the
server.
The server should be executed before any of the clients. It should be initiated as follows:

java Server server_port block_duration timeout

The client should accept the following argument:
- server_port: this is the port number being used by the server. This argument should be the same as the first argument of the server.

Note that, you do not have to specify the port to be used by the client. The client program should
allow the operating system to pick a random available port. Each client should be initiated in a
separate terminal as follows:

java Client server_port 

Note: The server and multiple clients should all be executed on the same machine on separate
terminals. In the client program, use 127.0.0.1 (localhost) as the server IP address.

## Commands supported
After a user is logged in, the client should support all the commands shown in the table below. For
the following, assume that commands were run by user Yoda. You may assume that during marking
the commands will be issued in the correct format as noted below with the appropriate arguments and
a single white space between arguments. The <message> can however contain multiple words
separated by white spaces. The message can contain uppercase characters (A-Z), lowercase characters
(a-z), digits (0-9) and special characters (~!@#$%^&*_-+=`|\(){}[]:;"'<>,.?/). For commands where
<user> is specified, the server should check if this <user> is present in the credentials file. If an entry
is not found (referred to as an invalid user), an appropriate error message should be displayed.

![image](https://user-images.githubusercontent.com/79550698/208603099-27e90adf-3f61-4cdb-8b9f-c6f4e3e746aa.png)
![image](https://user-images.githubusercontent.com/79550698/208602794-1f6bca45-4578-4a24-bf60-1a91ed90bf61.png)


Any command that is not listed above should result in an error message being displayed to the user.
The interaction with the user should be via the terminal (i.e., command line). All messages must be
displayed in the same terminal. There is no need to create separate terminals for messaging with
different users.
  
The server program should not print anything at the terminal. If you wish to print to the terminal for
debugging purposes, then include an optional debug flag (e.g. –d) as a command line argument for
the server. When this optional argument is included, your server can print debugging information to
the terminal. This same practice could also be employed for the client program.

## Peer to Peer Messaging
Some users may prefer to have some privacy during messaging. They may want to message their
friends directly without all their conversation being routed via the server. A peer-to-peer messaging
client is a good solution for this scenario. In addition to the above functionalities, you should
implement peer-to-peer messaging (also referred to as private messaging). Private messages should
be exchanged over a TCP connection between the two clients. Assume that client A wants to setup
a private chat with client B. Setting up a TCP connection between the two will require certain
information about B to be communicated to A via the server. We will not specify it here because there
are a few different ways to do that. We will leave the design to you.
  
To implement this functionality your client should support the following commands (in addition to
those listed in Section 3.3). For the following, assume that commands were run by user Yoda. The
notion of an invalid use is the same as described in Section 3.3. A p2p messaging session can only be
started with an online user.

  ![image](https://user-images.githubusercontent.com/79550698/208602968-0ba5c779-79b7-4c9b-ae5b-1a0bda44ddf4.png)

  
When a user logs off (or is timed out), any on-going p2p messaging session must conclude. A message
to this effect must be displayed to the other user involved in the p2p messaging session.
  
## Program design
![image](https://user-images.githubusercontent.com/79550698/208600689-a21cb2c4-69e5-4299-9488-210f089f0996.png)

Link to diagram: https://lucid.app/documents/view/753ae4e7-0b68-4008-844d-4728f8e6c807

The program consists of 4 classes. 
Server: Responsible for running the Server and accepting any new clients
Client: Responsible for running the Client and sending command to Server
ClientHandler: Part of Server, responsible for handling commands from Client
Account: Responsible for storing data about user accounts

## Data structure design
All the data about users are stored in the Account class and the data about active Clients are stored in the ClientHandler class in the arraylist “clientHandlers”.

## Application layer message format 
There is no strict message format, however when users send messages their message begins with their account name whereas Server messages are just plain messages with no format. This is enough to stop users trying to prose as the Server and altering the program as all their messages are distinguished by their messages beginning with their username.

## How does the system work?
An overview of the system is the server starts and waits for clients to connect via a TCP connection and then the server will control the flow of messages exchanged between clients.

A more in d valid the command is processed accordingly. Otherwise the time out timer will continue until thepth view is as follows: firstly, when the Server is started it will get three inputs from the command line arguments: the server port, block out time and timeout duration. Using the server port it will create a welcoming socket and populate the server accounts with all the entries in a file called “credentials.txt”. After all the setup it will start the server and wait for clients to connect via a TCP connection. When a client connects to the server, the server will accept this connection in its’ welcoming port and will create a new thread with it’s own socket to communicate with this client in the ClientHandler class. In the ClientHandler class it will detect this client is not logged in yet and will send the appropriate prompt for the client to login or create a new account. In the event the client fails to enter the correct password 3 times, then the ClientHander will lockout the account the client was attempting to login into for the Block out duration. When the Client has successfully logged in the timer for the time out will begin. Everytime the client sends a command to the ClientHander, the ClientHander will check if the command is valid. If the command is a valid command sent by the client. In the event the client didn’t send a valid command before the time out timer ran out the client program will be sent a time out message and the client program will terminate. 

For the Peer to Peer messaging the logic is as follows. Given there is two client logged in, when a client-A send the command “startprivate client-B” to the server, the server will send a request message to client-B and wait for a response from client-B. If client-B accepts this request, Client-B will start up a server socket and send a message containing this server port number to the server to which the server will send a message to Client-A containing Client-B server port number. Client-A using the received port number will connect to Client-B server socket via a TCP connection. Upon a successful connection both ends will remember the other client output and input stream from the socket. Both clients will also start up another thread to continuously listen for private messages. From here both clients can send messages without the need of going through the server. However each valid private message will send a valid command message server to maintain the time out timer. When a client in the private connection wants to disconnect or when either client logs out, both clients will try to send a message to the other client about the closure of the connection and then close the socket and all its streams.


## Design trade-offs considered and made
The main Design trade-off made was deciding to remove the welcoming port for peer to peer or private messaging. The reason behind this was to simplify the program as the specification said there will only be one private messaging session for each client. Hence it will be a lot simpler to remove the welcoming port and just use the port as the peer to peer socket.

## Limitation of Program
When the client disconnects mid way into logging in the entries are made null fields.
Since the specification stated “we will ONLY assume that users will exit by explicitly using the logout command” meaning the client will never disconnect mid way when logging in. Hence this limitation should never happen.
Only supports 1 peer to peer connection at a time. 
Since the specification stated “we will initiate at most one p2p messaging session from each client at any given time” which led to this limitation.
When waiting for the other peer to respond to the private messaging request, the peer who made the request cannot issue other commands until the other peer responds.
Since the specification stated “Setting up a TCP connection between the two will require certain information about B to be communicated to A via the server. ...We will leave the design to you.” Since this is part of the TCP connection process I decided on this limitation.

## Acknowledgement
The basic structure of the TCP Server and Client is based on the video by Wittcode instead of the provided starter code.
