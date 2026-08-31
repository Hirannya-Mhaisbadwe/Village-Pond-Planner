package com.example.Pond.Planning.Application.util;

public class TerrariumDecoder {

    private TerrariumDecoder() {
    }

    public static double decode(
            int red,
            int green,
            int blue
    ) {

        return
                (red * 256.0)
                        + green
                        + (blue / 256.0)
                        - 32768.0;
    }
}