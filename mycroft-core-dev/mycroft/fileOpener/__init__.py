
import socket
import pyaudio

class fileOpener():
    def __init__(self):
        # just for testing
        self.fileLocation = '/home/martin/Desktop/masterarbeit/sampleAudio/booting.wav'

    def openFile(self, uri):

        LoomoIP = '192.168.0.108'
        LoomoPort = 65433
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:

            s.connect((LoomoIP, LoomoPort))
            with open (uri, 'rb') as f:
                message = f.read()
                s.sendall(message)
            s.close()

