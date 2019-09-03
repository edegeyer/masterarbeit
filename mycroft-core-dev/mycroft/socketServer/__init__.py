from threading import Thread
import socket
import pyaudio
import sys

myHOST = '192.168.0.109'  # Standard loopback interface address (localhost)
myPORT = 65432  # Port to listen on (non-privileged ports are > 1023)

class socketServer(Thread):

    def __init__(self):
        super(socketServer, self).__init__()
        thread = Thread(target=self.createListener)
        thread.daemon = True
        thread.start()


    def createListener(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind((myHOST, myPORT))
        p = pyaudio.PyAudio()
        audiostream = p.open(format=pyaudio.paInt16, channels=1, rate=16000, output=True)
        #audiostream = self.audio.open(format=pyaudio.paInt16, channels=1, rate=16000, input=True)

        print("created socket at: ", socket.gethostname(), " ", myPORT)
        s.listen(1)
        print("now listening...")
        while True:
            conn, addr = s.accept()
            #self.print_lock.acquire()
            print("Connected to: ", addr)
            self.isStreaming = True
            while True:
                data = conn.recv(7168)
                audiostream.write(data)
                if not data:
                    print("Bye")
                    self.isStreaming = False
                    break
                elif data == 'killsrv':
                    conn.close()
                    sys.exit()