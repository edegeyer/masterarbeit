from mycroft.messagebus.message import Message
import  mycroft.messagebus.client.ws as ws


class fileOpener():
    def __init__(self):
        self.fileLocation = '/home/martin/Desktop/masterarbeit/sampleAudio/booting.wav'

    def openFile(self, uri, bus):
        chunk = 2048
        f = open(self.fileLocation, "rb")
        #bus.emit(Message("HELLO", "HELLO"))
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

        #bus.emit(Message )
        #print("uri is: ", uri)
        # TODO: einfaches emit auf bus

