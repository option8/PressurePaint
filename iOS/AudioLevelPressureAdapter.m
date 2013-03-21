//
//  AudioLevelPressureAdapter.m

/*
Copyright (c) 2012, Charles Mangin - Option8, LLC
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

Neither the name of Option8, LLC nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/



#include <AudioToolbox/AudioToolbox.h>
#include <math.h>
#import "AudioLevelPressureAdapter.h"


/*
 * Sends constant tone for pen to modulate
 */
static void HandleOutputBuffer (
    void                *inUserData,
    AudioQueueRef       outAQ,
    AudioQueueBufferRef outBuffer
) {
    AudioLevelPressureAdapter *adapter = (AudioLevelPressureAdapter *)inUserData;    
    [adapter generateTone:outBuffer];
    AudioQueueEnqueueBuffer(outAQ, outBuffer, 0, NULL);
}


/*
 * Samples mic input for pen modulation
 */
static void HandleInputBuffer(
    void                *inUserData,
    AudioQueueRef       inAudioQueue,
    AudioQueueBufferRef inBuffer,
    const AudioTimeStamp *inStartTime,
    UInt32              inNumPackets,
    const AudioStreamPacketDescription *inPacketDesc
) {
    AudioLevelPressureAdapter *adapter = (AudioLevelPressureAdapter *) inUserData;
    AudioQueueEnqueueBuffer(inAudioQueue, inBuffer, 0, NULL);
}



@implementation AudioLevelPressureAdapter

@synthesize notificationDelegate;

- (id) init
{
    return [self initWithWidth: 10];
}

- (id) initWithWidth: (int)w
{    
    self = [super init];
    
    width = w;
    maInputs = [NSMutableArray array];
    
    fstep = kBytesPerFrame * M_PI * kFrequency / kSampleRate;
    
    if (self != nil)
    {
        inputFormat.mSampleRate             = kInputSampleRate;
        inputFormat.mFormatID               = kAudioFormatLinearPCM;
        inputFormat.mFormatFlags            = kAudioFormatFlagIsSignedInteger | kAudioFormatFlagIsPacked;
        inputFormat.mFramesPerPacket        = 1;
        inputFormat.mChannelsPerFrame       = 1;
        inputFormat.mBitsPerChannel         = 16;
        inputFormat.mBytesPerPacket         = kBytesPerFrame;
        inputFormat.mBytesPerFrame          = kBytesPerFrame;
        
        AudioQueueNewInput(
            &inputFormat,
            HandleInputBuffer,
            self,
            NULL,
            NULL,
            0,
            &inputQueue
        );
        
        UInt32 sizeOfRecordingFormatASBDStruct = sizeof (inputFormat);
        AudioQueueGetProperty(inputQueue,
                              kAudioQueueProperty_StreamDescription,
                              &inputFormat,
                              &sizeOfRecordingFormatASBDStruct);

        outputFormat.mSampleRate             = kSampleRate;
        outputFormat.mFormatID               = kAudioFormatLinearPCM;
        outputFormat.mFormatFlags            = kAudioFormatFlagIsSignedInteger | kAudioFormatFlagIsPacked;
        outputFormat.mFramesPerPacket        = 1;
        outputFormat.mChannelsPerFrame       = 1;
        outputFormat.mBitsPerChannel         = 16;
        outputFormat.mBytesPerPacket         = kBytesPerFrame;
        outputFormat.mBytesPerFrame          = kBytesPerFrame;
                
        AudioQueueNewOutput(
            &outputFormat,
            HandleOutputBuffer,
            self,
            NULL,
            NULL,
            0,
            &outputQueue
        );
        
        
        UInt32 bufferSizeBytes = kBufferSizeFrames * outputFormat.mBytesPerFrame;
        
        for (int i=0; i<kNumBuffers; i++)
        {
            AudioQueueAllocateBuffer(outputQueue, bufferSizeBytes, &outputBuffers[i]);
            HandleOutputBuffer(self, outputQueue, outputBuffers[i]);
        }
        
        [self startGeneratingTone];
        [self enableLevelMetering];
    }
    
    return self;
}

- (void) incrementStartingPacketNumberBy:(UInt32)inNumPackets
{
    startingPacketNumber += inNumPackets;
}

- (void) setNotificationDelegate:(id)inDelegate
{
    notificationDelegate = inDelegate;
}

- (BOOL) isRunning
{
    UInt32 isRunning;
    UInt32 propertySize = sizeof (UInt32);
    OSStatus result;
    
    result = AudioQueueGetProperty (inputQueue, kAudioQueueProperty_IsRunning, &isRunning, &propertySize);
    
    if( result != noErr)
    {
        return false;
    }
    else
    {
        return isRunning;
    }
}

- (void) enableLevelMetering
{
    audioLevels = (AudioQueueLevelMeterState *) calloc (sizeof (AudioQueueLevelMeterState), inputFormat.mChannelsPerFrame);
    UInt32 trueValue = true;
    AudioQueueSetProperty(inputQueue, kAudioQueueProperty_EnableLevelMetering, &trueValue, sizeof (UInt32));
}

- (void) getAudioLevels:(Float32 *)levels peakLevels:(Float32 *)peakLevels
{
    UInt32 propertySize = inputFormat.mChannelsPerFrame * sizeof (AudioQueueLevelMeterState);
    AudioQueueGetProperty(inputQueue, (AudioQueuePropertyID) kAudioQueueProperty_CurrentLevelMeter, audioLevels, &propertySize);
    levels[0]       = audioLevels[0].mAveragePower;
    peakLevels[0]   = audioLevels[0].mPeakPower;
}

- (void) dealloc
{
    [super dealloc];
}

- (void) monitor
{
    OSStatus error = AudioQueueStart(inputQueue, NULL);
    if (error != noErr) {
        NSLog(@"Error starting inputQueue: %ld", error);
    }
}

-(BOOL) isPossiblyConnected
{
    return YES; // Always show connected
    
    CFStringRef state = nil;
    UInt32 propertySize = sizeof(CFStringRef);
    AudioSessionInitialize(NULL, NULL, NULL, NULL);
    AudioSessionGetProperty(kAudioSessionProperty_AudioRoute, &propertySize, &state);
    
<<<<<<< HEAD
    if((NSString *)state == @"MicrophoneWired") {
=======
    return YES; // Always show connected
    
/*    if((NSString *)state == @"MicrophoneWired") {
>>>>>>> updated build info for new xcode.
        return YES;
    } else {
        return NO;
    }*/
}

-(void) generateTone:(AudioQueueBuffer *)buffer
{
  
  SInt16 *caOutBuffer = (SInt16*)buffer->mAudioData;
    
    buffer->mAudioDataByteSize = kBufferSizeFrames * outputFormat.mBytesPerFrame;
    
    for (int s=0; s<kBufferSizeFrames*2; s+=2)
    {
        float sample = sinf(phase);
        short sampleI = (int)(sample * 32767.0);
        
        caOutBuffer[s] = sampleI;
        
        phase += fstep;
    }
    
    phase = fmodf(phase, 2 * M_PI);
  
}

- (void) startGeneratingTone
{
    OSStatus status = AudioQueueSetParameter(outputQueue, kAudioQueueParam_Volume, 1.0);
    status = AudioQueueStart(outputQueue, NULL);
}

@end
