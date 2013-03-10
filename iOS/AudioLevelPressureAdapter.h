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


#import <Foundation/Foundation.h>
#import <AudioToolbox/AudioToolbox.h>
#include <math.h>

#define kBufferSizeFrames   512
#define kNumBuffers         4
#define kFrequency          4000
#define kSampleRate         44100
#define kBytesPerFrame      2

@interface AudioLevelPressureAdapter : NSObject {
    AudioQueueRef inputQueue;
    AudioQueueRef outputQueue;
    AudioQueueBufferRef outputBuffers[kNumBuffers];
    AudioStreamBasicDescription audioFormat;
    AudioQueueLevelMeterState *audioLevels;
    SInt64 startingPacketNumber;
    id notificationDelegate;
    
    // For moving average filtering
    int width;
    NSMutableArray *maInputs;
    
    // For wave generation
    float fstep;
    float phase;
}

@property (nonatomic, retain) id        notificationDelegate;

-(id) init;
-(id) initWithWidth: (int) w;

-(void) incrementStartingPacketNumberBy: (UInt32) inNumPackets;
-(void) setNotificationDelegate: (id) inDelegate;
-(void) enableLevelMetering;
-(void) getAudioLevels: (Float32 *) levels peakLevels: (Float32 *) peakLevels;
-(BOOL) isRunning;
-(void) monitor;
-(BOOL) isPossiblyConnected;

-(void) generateTone: (AudioQueueBuffer *)buffer;
-(void) startGeneratingTone;

@end
