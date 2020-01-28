# ReadySetGo
Program to play Go against computer players, or to watch Go being played by two computer players.

To get the program up and running, open a command line terminal.
1. First make a clone of this repository by typing in the following lines in the terminal and hitting enter after each line:
 * mkdir goGameDirectory
 * cd goGameDirectory
 * git clone https://github.com/ElineVerbon/FinalAssignment1.git
 * cd FinalAssignment1
2. Then, start the server by typing this line in the command line and hitting enter:
 * ./server

_Follow the prompts in the terminal, typing in an IP address (or 'localhost') and a port number_

3. Open a new command line window and start a client by typing __one of these lines__ in the command line and hitting enter:
 * ./human
 * ./smart1
 * ./smart2
 * ./smart3

_Follow the prompts in the terminal. If you open 'human', you will need to type in your moves. In the case of 'smartX', you only participate at the start of the exchange (entering the IP address and the port number you want to connect with). Afterwards, the computer will take over and play the game to the end._

4. Open another new command line window and start another client by typing __one of these lines__ in the command line and hitting enter:
 * ./human
 * ./smart1
 * ./smart2
 * ./smart3

_Again, follow the prompts in the terminal. If you open 'human', you will need to type in your moves. In the case of 'smartx', you will indicate the server you want to connect with, but afterwards the computer will take over._

5. Once a client indicates that it has closed the connection, you can type 'ctrl c' to stop the execution and to start another client or server, if wanted.

If wanted, you can add more clients. For every two clients added, a game will be started. Clients on other computers running the same program (or a different program with the same communication protocol) can also connect to your server. Similarly, you can connect to another computer. 

Have fun!
