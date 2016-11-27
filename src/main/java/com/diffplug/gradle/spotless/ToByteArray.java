package com.diffplug.gradle.spotless;

interface ToByteArray {
  /**
   * Returns a byte array representation of everything inside this `SerializableFileFilter`.
   *
   * The main purpose of this interface is to allow other interfaces to extend it, which in turn
   * ensures that one can't instantiate the extending interfaces with lambda expressions, which are
   * notoriously difficult to serialize and deserialize properly. (See
   * `SerializableFileFilterImpl.SkipFilesNamed` for an example of how to make a serializable
   * subclass.)
   */
  byte[] toBytes();
}
