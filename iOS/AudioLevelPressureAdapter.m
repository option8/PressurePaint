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

static void recordingCallback(void *inUserData,AudioQueueRef inAudioQueue,AudioQueueBufferRef inBuffer,
                              const AudioTimeStamp *inStartTime,UInt32 inNumPackets,const AudioStreamPacketDescription *inPacketDesc)
{
    AudioLevelPressureAdapter *adapter = (AudioLevelPressureAdapter *) inUserData;
    
//    NSLog(@"Callback");
    
    // Monitor the levels on input
    if(inNumPackets > 0)
    {
//        NSLog(@"Detected source, sample levels");
    }
    
    // Reset the buffer and prepare for next packet
    if([adapter isRunning])
    {
        AudioQueueEnqueueBuffer(inAudioQueue, inBuffer, 0, NULL);
    }
}

@implementation AudioLevelPressureAdapter

@synthesize queueObject;
@synthesize audioFormat;
@synthesize audioLevels;
@synthesize startingPacketNumber;
@synthesize notificationDelegate;

- (id) init
{
    return [self initWithWidth: 10];
}

- (id) initWithWidth: (int)w
{
//    NSLog(@"Loading pressure adapter");
    
    self = [super init];
    
    width = w;
    maInputs = [NSMutableArray array];
    
    if (self != nil)
    {
       // audioFormat.mSampleRate             = 44100.00;
        audioFormat.mSampleRate             = 1000.00;
        audioFormat.mFormatID               = kAudioFormatLinearPCM;
        audioFormat.mFormatFlags            = kAudioFormatFlagIsSignedInteger | kAudioFormatFlagIsPacked;
        audioFormat.mFramesPerPacket        = 1;
        audioFormat.mChannelsPerFrame       = 1;
        audioFormat.mBitsPerChannel         = 16;
        audioFormat.mBytesPerPacket         = 2;
        audioFormat.mBytesPerFrame          = 2;
        
        AudioQueueNewInput(&audioFormat,recordingCallback,self,NULL,NULL,0,&queueObject);
        
        UInt32 sizeOfRecordingFormatASBDStruct = sizeof (audioFormat);
        
        AudioQueueGetProperty(queueObject, kAudioQueueProperty_StreamDescription, &audioFormat, &sizeOfRecordingFormatASBDStruct);
        
        [self enableLevelMetering];
//        NSLog(@"Initialized");
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
    
    result = AudioQueueGetProperty (queueObject, kAudioQueueProperty_IsRunning, &isRunning, &propertySize);
    
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
    self.audioLevels = (AudioQueueLevelMeterState *) calloc (sizeof (AudioQueueLevelMeterState), audioFormat.mChannelsPerFrame);
    UInt32 trueValue = true;
    AudioQueueSetProperty(self.queueObject, kAudioQueueProperty_EnableLevelMetering, &trueValue, sizeof (UInt32));
}

- (void) getAudioLevels:(Float32 *)levels peakLevels:(Float32 *)peakLevels
{
    UInt32 propertySize = audioFormat.mChannelsPerFrame * sizeof (AudioQueueLevelMeterState);
    AudioQueueGetProperty(self.queueObject, (AudioQueuePropertyID) kAudioQueueProperty_CurrentLevelMeter, self.audioLevels, &propertySize);
    levels[0]       = self.audioLevels[0].mAveragePower;
    peakLevels[0]   = self.audioLevels[0].mPeakPower;
}

- (void) dealloc
{
    [super dealloc];
}

- (void) monitor
{
    AudioQueueStart(queueObject, NULL);
//    NSLog(@"Monitoring started");
}

-(BOOL) isPossiblyConnected
{
    CFStringRef state = nil;
    UInt32 propertySize = sizeof(CFStringRef);
    AudioSessionInitialize(NULL, NULL, NULL, NULL);
    OSStatus status = AudioSessionGetProperty(kAudioSessionProperty_AudioRoute, &propertySize, &state);
    
    return YES; // Always show connected
    
    if((NSString *)state == @"MicrophoneWired") {
        return YES;
    } else {
        return NO;
    }
}
@end
