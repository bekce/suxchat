# suxchat
> A Nifty Java Client-Server Chat Tool

Back in the days when we were giving consultancy service to a company which limited the internet usage of its employees, 
I’ve developed a simple solution for our inside-company basic communication problem with a nifty tool written in Java. 

![screenshot](https://cloud.githubusercontent.com/assets/5337921/23701860/4a262f7c-0401-11e7-85c9-427e6ff083f6.png)

### Features

- Keep alive mode, which prevents connection loss due to inactivity for a long period.  
- Operator mode to kick out users, which is enabled with a special password.  
- Entrance password mode can be enabled to lock out strangers.  
- Normally all messages are broadcasted to all clients and private messaging feature allows secret communication between any two client.  
- Listing of currently logged on users.  
- A nice Swing GUI for the client, which flashes its icon in dock when it receives a new message.  
- Uses UTF-8 as its string encoding, so you can use accented characters.  
- Messages are time stamped by the server.  
- Communication is done via plain sockets.  
- Configuration is done via properties files.  
- Unlike many other tools, there’s no bug and runs perfectly.

### Instructions for Server

1.  [Download suxchat-server.jar](https://github.com/bekce/suxchat/raw/master/dist/suxchat-0.3.7-server.jar)
2.  Run it by double-clicking on it or typing `java -jar suxchat-0.3.7-server.jar`.  
    Server does not have a UI, it will work as a backgroud process.

### Instructions for Client

1.  [Download suxchat-client.jar](https://github.com/bekce/suxchat/raw/master/dist/suxchat-0.3.7-client.jar)
2.  Run it by double-clicking on it or typing `java -jar suxchat-0.3.7-client.jar`.

### Commands Reference

##### Regular Commands

- `message` send _message_ to everyone online
- `U` view online users
- `P user message` Send a private _message_ to _user_

##### Special Commands

- `AUTH password` authorize yourself as operator with _password_  
    (Default password: 1122, configurable in `server.properties`)
- `K user` (requires operator) kick _user_ from the server
