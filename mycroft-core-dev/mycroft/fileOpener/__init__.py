# class that's used to open the local wave file, that's generated as spoken answer

import socket

class fileOpener():
    def __init__(self):
        pass

    def openFile(self, uri):
        # TODO: set the IP to the one that's currently used by Loomo
        #LoomoIP = '192.168.0.108'
        LoomoIP = "192.168.43.52"
        LoomoPort = 65433
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:

            s.connect((LoomoIP, LoomoPort))
            with open (uri, 'rb') as f:
                message = f.read()
                s.sendall(message)
            s.close()

