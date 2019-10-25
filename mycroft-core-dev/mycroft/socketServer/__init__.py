# class that is used to get the audiostream from Loomo
# for further use, the input gets played via connected speakers
# requires setup of the microphone with pulseaudio, so that the output get's redirected to it
# recommended to connect something to the aux output of the device, otherwise loopback input will be created


from threading import Thread
import socket
import pyaudio
import sys

# TODO: set IP to the own IP in the network
myHOST = "192.168.43.138"
myPORT = 65432  # Port to listen on (non-privileged ports are > 1023)

class socketServer(Thread):

    # creates the class and opens a new thread for listening by calling the createListener function
    def __init__(self):
        super(socketServer, self).__init__()
        thread = Thread(target=self.createListener)
        thread.daemon = True
        thread.start()

    # function opens the socket and plays the received audio
    def createListener(self):
        self.mysocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.mysocket.bind((myHOST, myPORT))
        p = pyaudio.PyAudio()
        audiostream = p.open(format=pyaudio.paInt16, channels=1, rate=16000, output=True)

        print("created socket at: ", socket.gethostname(), " ", myPORT)
        self.mysocket.listen(1)
        print("now listening...")
        while True:
            conn, addr = self.mysocket.accept()
            print("Connected to: ", addr)
            self.isStreaming = True
            while True:
                data = conn.recv(1024)
                audiostream.write(data)
                if not data:
                    print("Bye")
                    self.isStreaming = False
                    break
                elif data == 'killsrv':
                    conn.close()
                    sys.exit()