from mycroft.messagebus.message import Message
import  mycroft.messagebus.client.ws as ws
import socket

class fileOpener():
    def __init__(self):
        self.fileLocation = '/home/martin/Desktop/masterarbeit/sampleAudio/booting.wav'

    def openFile(self):#, uri, bus):
        chunk = 2048
        #bus.emit(Message("HELLO", "HELLO"))
        # TODO: Client mit dem Server verbinden -> dieser kann bei jedem Request verbinden, da der Server uenendlich läuft
        # TODO: verschicken der Nachricht

        LoomoIP = '192.168.0.108'
        LoomoPort = 65433
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            #with open(self.fileLocation, 'rb') as f:
            #    s.sendall(f.read())
            s.connect((LoomoIP, LoomoPort))
            with open (self.fileLocation, 'rb') as f:
                message = f.read()
                s.sendall(message)
            #    print(message)
            #s.sendall(b'Hello World')
            #s.sendall(f.read())
            #for line in f:
            #    s.sendall(line)
            #    print(line)
            #print(f.read())
            # Connection can be closed, as soon as the message is send over
            s.close()
            #data = s.recv(1024)
        #print('Received', repr(data))


'''
        bus.emit(Message(
            "OPENFILE",
            {'uri': uri}))
        while True:
            piece = f.read(chunk)
            #print(piece)
            stringPiece = str(piece)
            bus.emit(Message(
                "Audio",
                {'action': stringPiece}))
            if piece == b'':
                bus.emit(Message(
                    "Audio",
                    {'action': "end"}))
                break

        print(f.read())
'''
        #bus.emit(Message )
        #print("uri is: ", uri)
        # TODO: einfaches emit auf bus

