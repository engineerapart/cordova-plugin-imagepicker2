//
//  SOSPicker.h
//  SyncOnSet
//
//  Created by Christopher Sullivan on 10/25/13.
//
//

#import <Cordova/CDVPlugin.h>

// It would be great to integrate changes made by this guy:
// https://github.com/nixplay/cordova-imagePicker
// The Android library handling is much better.

@interface SOSPicker : CDVPlugin < UINavigationControllerDelegate, UIScrollViewDelegate>

@property (copy)   NSString* callbackId;

- (void) getPictures:(CDVInvokedUrlCommand *)command;
- (void) hasReadPermission:(CDVInvokedUrlCommand *)command;
- (void) requestReadPermission:(CDVInvokedUrlCommand *)command;

- (UIImage*)imageByScalingNotCroppingForSize:(UIImage*)anImage toSize:(CGSize)frameSize;

@property (nonatomic, assign) NSInteger width;
@property (nonatomic, assign) NSInteger height;
@property (nonatomic, assign) NSInteger maximumImagesCount;
@property (nonatomic, assign) NSInteger quality;
@property (nonatomic, assign) NSInteger outputType;
@property (nonatomic, assign) BOOL allow_video;
@property (nonatomic, assign) NSString* title;
@property (nonatomic, assign) NSString* message;

@end
