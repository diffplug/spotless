#!/usr/bin/env bash

fruit="apple"

case $fruit in
    "apple")
        echo "This is a red fruit."
    ;;
    "banana")
        echo "This is a yellow fruit."
;;
    "orange")
        echo "This is an orange fruit."
      ;;
    *)
          echo "Unknown fruit."
        ;;
esac

      echo "This is some text." >       output.txt
