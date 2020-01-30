# ReadySetGo
Program to play Go against computer players, or to watch Go being played by two computer players.

Take care: this program  __only works if you have Java 11 installed__. Please install Java 11 if necessary.

To get the program up and running, open a command line terminal.
1. First make a clone of this repository by typing in the following lines in the terminal and hitting enter after each line:

`mkdir goGameDirectory`

`cd goGameDirectory`

`git clone https://github.com/ElineVerbon/ReadySetGo.git`

`cd ReadySetGo`

2. Then, start the server by typing this line in the command line and hitting enter:

__Mac users:__

`./server`


__Windows users:__

`server.bat`

_Follow the prompts in the terminal, typing in the board size and a port number_

3. Open a new command line window and start a client by typing  __one of these lines__  in the command line and hitting enter:

__Mac users:__

`./humanPlayer`			_This will start a player of which you can decide the moves._

`./smartComputerPlayer`	_This will start an automized player that uses a smart(er) strategy._

`./stupidComputerPlayer` 	_This will start an automized player that uses a less smart stategy._


__Windows users:__

`humanPlayer.bat`			_This will start a player of which you can decide the moves._

`smartComputerPlayer.bat`	_This will start an automized player that uses a smart(er) strategy._

`stupidComputerPlayer.bat` 	_This will start an automized player that uses a less smart stategy._

_Follow the prompts in the terminal. If you open the humanPlayer, you will need to type in your moves. In the case of one of the computer players, you only participate at the start of the exchange (entering the IP address and the port number you want to connect with). Afterwards, the computer will take over and play the game to the end._

4. Open another new command line window and repeat step 3, choosing the same or a different player according to your wish.

5. Once a client indicates that it has closed the connection, you can type 'ctrl c' to stop the execution and to start another client or server, if wanted.

If wanted, you can add more clients. For every two clients added, a game will be started. Clients on other computers running the same program (or a different program with the same communication protocol) can also connect to your server. Similarly, you can connect as a client to another computer running a server with the same communication protocol. 

Have fun!
