import pyaudio
import wave

fileLocation = '../sampleAudio/turnLeft.wav'

'''
in_file = open(fileLocation, 'rb') # open as [r]eading [b]inary

chunk = 4096
wf = wave.open(fileLocation, 'rb')
p = pyaudio.PyAudio()
stream = p.open(format=p.get_format_from_width(wf.getsampwidth()),
                channels = wf.getnchannels(),
                rate=wf.getframerate(),
                output=True)
data = wf.readframes(chunk)
while data != '':
    stream.write(data)
    print(data)

    data = wf.readframes(chunk)

stream.close()
p.terminate()

'''

f = open(fileLocation, "rb")
print(f.read())
p = pyaudio.PyAudio()
stream = p.open(format=pyaudio.paInt16, channels=1, rate=16000, output=True)
stream.write(f.read())


