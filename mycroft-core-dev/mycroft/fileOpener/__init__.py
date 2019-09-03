from mycroft.messagebus.message import Message

class fileOpener():
    def __init__(self):
        self.fileLocation = '/home/martin/Desktop/masterarbeit/sampleAudio/turnLeft.wav'

    def openFile(self, uri, bus):
        chunk = 128
        f = open(self.fileLocation, "rb")
        bus.emit(Message(
            "AUDIO",
            {'action': uri}))
        while True:
            piece  = f.read(chunk)
            print(piece)
            stringPiece = str(piece)
            bus.emit(Message(
                "AUDIO",
                {'action': stringPiece}))
            if piece == b'':
                print("end of file")
                break

        #print(f.read())

        #bus.emit(Message )
        #print("uri is: ", uri)
        # TODO: einfaches emit auf bus

