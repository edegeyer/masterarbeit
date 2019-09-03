from mycroft.messagebus.message import Message

class fileOpener():
    def __init__(self):
        self.fileLocation = '/home/martin/Desktop/masterarbeit/sampleAudio/turnLeft.wav'

    def openFile(self):#, uri, bus):
        chunk = 512
        f = open(self.fileLocation, "rb")
        while True:
            piece = myfile = f.read(chunk)
            print(piece)
            if piece == b'':
                print("end of file")
                break

        #print(f.read())
        #bus.emit(Message(
        #    "AUDIO",
        #    {'action': uri}))
        #bus.emit(Message )
        #print("uri is: ", uri)
        # TODO: einfaches emit auf bus

