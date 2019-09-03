

class fileOpener():
    def __init__(self):
        self.fileLocation = '/home/martin/Desktop/masterarbeit/sampleAudio/turnLeft.wav'

    def openFile(self):
        f = open(self.fileLocation, "rb")
        print(f.read())
        # TODO: einfaches emit auf bus

