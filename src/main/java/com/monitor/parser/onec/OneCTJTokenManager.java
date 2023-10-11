package com.monitor.parser.onec;

import com.monitor.parser.Token;
import com.monitor.parser.onec.OneCTJConstants;

/**
 * Token Manager.
 */
@SuppressWarnings("unused")
public class OneCTJTokenManager implements OneCTJConstants {

    /**
     * Debug output.
     */
    public java.io.PrintStream debugStream = System.out;

    /**
     * Set debug output.
     */
    public void setDebugStream(java.io.PrintStream ds) {
        debugStream = ds;
    }

    private final int jjStopStringLiteralDfa_1(int pos, long active0, long active1) {
        switch (pos) {
            case 0:
                if ((active0 & 0x3f0000L) != 0L) {
                    jjmatchedKind = 22;
                    return 1;
                }
                return -1;
            case 1:
                if ((active0 & 0x3f0000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 1;
                    return 1;
                }
                return -1;
            case 2:
                if ((active0 & 0x2f0000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 2;
                    return 1;
                }
                if ((active0 & 0x100000L) != 0L) {
                    return 1;
                }
                return -1;
            case 3:
                if ((active0 & 0x2f0000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 3;
                    return 1;
                }
                return -1;
            case 4:
                if ((active0 & 0xc0000L) != 0L) {
                    return 1;
                }
                if ((active0 & 0x230000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 4;
                    return 1;
                }
                return -1;
            case 5:
                if ((active0 & 0x230000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 5;
                    return 1;
                }
                return -1;
            case 6:
                if ((active0 & 0x10000L) != 0L) {
                    return 1;
                }
                if ((active0 & 0x220000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 6;
                    return 1;
                }
                return -1;
            case 7:
                if ((active0 & 0x220000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 7;
                    return 1;
                }
                return -1;
            case 8:
                if ((active0 & 0x220000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 8;
                    return 1;
                }
                return -1;
            case 9:
                if ((active0 & 0x200000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 9;
                    return 1;
                }
                if ((active0 & 0x20000L) != 0L) {
                    return 1;
                }
                return -1;
            case 10:
                if ((active0 & 0x200000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 10;
                    return 1;
                }
                return -1;
            case 11:
                if ((active0 & 0x200000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 11;
                    return 1;
                }
                return -1;
            case 12:
                if ((active0 & 0x200000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 12;
                    return 1;
                }
                return -1;
            case 13:
                if ((active0 & 0x200000L) != 0L) {
                    jjmatchedKind = 22;
                    jjmatchedPos = 13;
                    return 1;
                }
                return -1;
            default:
                return -1;
        }
    }

    private final int jjStartNfa_1(int pos, long active0, long active1) {
        return jjMoveNfa_1(jjStopStringLiteralDfa_1(pos, active0, active1), pos + 1);
    }

    private int jjStopAtPos(int pos, int kind) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        return pos + 1;
    }

    private int jjMoveStringLiteralDfa0_1() {
        switch (curChar) {
            case 10:
                return jjStopAtPos(0, 60);
            case 44:
                return jjStopAtPos(0, 63);
            case 61:
                return jjStopAtPos(0, 62);
            case 67:
                return jjMoveStringLiteralDfa1_1(0x10000L);
            case 76:
                return jjMoveStringLiteralDfa1_1(0x80000L);
            case 83:
                return jjMoveStringLiteralDfa1_1(0x100000L);
            case 87:
                return jjMoveStringLiteralDfa1_1(0x200000L);
            case 101:
                return jjMoveStringLiteralDfa1_1(0x20000L);
            case 108:
                return jjMoveStringLiteralDfa1_1(0x40000L);
            default:
                return jjMoveNfa_1(0, 0);
        }
    }

    private int jjMoveStringLiteralDfa1_1(long active0) {
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(0, active0, 0L);
            return 1;
        }
        switch (curChar) {
            case 97:
                return jjMoveStringLiteralDfa2_1(active0, 0x200000L);
            case 107:
                return jjMoveStringLiteralDfa2_1(active0, 0x40000L);
            case 111:
                return jjMoveStringLiteralDfa2_1(active0, 0x90000L);
            case 113:
                return jjMoveStringLiteralDfa2_1(active0, 0x100000L);
            case 115:
                return jjMoveStringLiteralDfa2_1(active0, 0x20000L);
            default:
                break;
        }
        return jjStartNfa_1(0, active0, 0L);
    }

    private int jjMoveStringLiteralDfa2_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(0, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(1, active0, 0L);
            return 2;
        }
        switch (curChar) {
            case 99:
                return jjMoveStringLiteralDfa3_1(active0, 0xa0000L);
            case 105:
                return jjMoveStringLiteralDfa3_1(active0, 0x200000L);
            case 108:
                if ((active0 & 0x100000L) != 0L) {
                    return jjStartNfaWithStates_1(2, 20, 1);
                }
                break;
            case 110:
                return jjMoveStringLiteralDfa3_1(active0, 0x10000L);
            case 115:
                return jjMoveStringLiteralDfa3_1(active0, 0x40000L);
            default:
                break;
        }
        return jjStartNfa_1(1, active0, 0L);
    }

    private int jjMoveStringLiteralDfa3_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(1, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(2, active0, 0L);
            return 3;
        }
        switch (curChar) {
            case 97:
                return jjMoveStringLiteralDfa4_1(active0, 0x20000L);
            case 107:
                return jjMoveStringLiteralDfa4_1(active0, 0x80000L);
            case 114:
                return jjMoveStringLiteralDfa4_1(active0, 0x40000L);
            case 116:
                return jjMoveStringLiteralDfa4_1(active0, 0x210000L);
            default:
                break;
        }
        return jjStartNfa_1(2, active0, 0L);
    }

    private int jjMoveStringLiteralDfa4_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(2, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(3, active0, 0L);
            return 4;
        }
        switch (curChar) {
            case 67:
                return jjMoveStringLiteralDfa5_1(active0, 0x200000L);
            case 99:
                if ((active0 & 0x40000L) != 0L) {
                    return jjStartNfaWithStates_1(4, 18, 1);
                }
                break;
            case 101:
                return jjMoveStringLiteralDfa5_1(active0, 0x10000L);
            case 108:
                return jjMoveStringLiteralDfa5_1(active0, 0x20000L);
            case 115:
                if ((active0 & 0x80000L) != 0L) {
                    return jjStartNfaWithStates_1(4, 19, 1);
                }
                break;
            default:
                break;
        }
        return jjStartNfa_1(3, active0, 0L);
    }

    private int jjMoveStringLiteralDfa5_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(3, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(4, active0, 0L);
            return 5;
        }
        switch (curChar) {
            case 97:
                return jjMoveStringLiteralDfa6_1(active0, 0x20000L);
            case 111:
                return jjMoveStringLiteralDfa6_1(active0, 0x200000L);
            case 120:
                return jjMoveStringLiteralDfa6_1(active0, 0x10000L);
            default:
                break;
        }
        return jjStartNfa_1(4, active0, 0L);
    }

    private int jjMoveStringLiteralDfa6_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(4, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(5, active0, 0L);
            return 6;
        }
        switch (curChar) {
            case 110:
                return jjMoveStringLiteralDfa7_1(active0, 0x200000L);
            case 116:
                if ((active0 & 0x10000L) != 0L) {
                    return jjStartNfaWithStates_1(6, 16, 1);
                }
                return jjMoveStringLiteralDfa7_1(active0, 0x20000L);
            default:
                break;
        }
        return jjStartNfa_1(5, active0, 0L);
    }

    private int jjMoveStringLiteralDfa7_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(5, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(6, active0, 0L);
            return 7;
        }
        switch (curChar) {
            case 105:
                return jjMoveStringLiteralDfa8_1(active0, 0x20000L);
            case 110:
                return jjMoveStringLiteralDfa8_1(active0, 0x200000L);
            default:
                break;
        }
        return jjStartNfa_1(6, active0, 0L);
    }

    private int jjMoveStringLiteralDfa8_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(6, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(7, active0, 0L);
            return 8;
        }
        switch (curChar) {
            case 101:
                return jjMoveStringLiteralDfa9_1(active0, 0x200000L);
            case 110:
                return jjMoveStringLiteralDfa9_1(active0, 0x20000L);
            default:
                break;
        }
        return jjStartNfa_1(7, active0, 0L);
    }

    private int jjMoveStringLiteralDfa9_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(7, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(8, active0, 0L);
            return 9;
        }
        switch (curChar) {
            case 99:
                return jjMoveStringLiteralDfa10_1(active0, 0x200000L);
            case 103:
                if ((active0 & 0x20000L) != 0L) {
                    return jjStartNfaWithStates_1(9, 17, 1);
                }
                break;
            default:
                break;
        }
        return jjStartNfa_1(8, active0, 0L);
    }

    private int jjMoveStringLiteralDfa10_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(8, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(9, active0, 0L);
            return 10;
        }
        switch (curChar) {
            case 116:
                return jjMoveStringLiteralDfa11_1(active0, 0x200000L);
            default:
                break;
        }
        return jjStartNfa_1(9, active0, 0L);
    }

    private int jjMoveStringLiteralDfa11_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(9, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(10, active0, 0L);
            return 11;
        }
        switch (curChar) {
            case 105:
                return jjMoveStringLiteralDfa12_1(active0, 0x200000L);
            default:
                break;
        }
        return jjStartNfa_1(10, active0, 0L);
    }

    private int jjMoveStringLiteralDfa12_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(10, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(11, active0, 0L);
            return 12;
        }
        switch (curChar) {
            case 111:
                return jjMoveStringLiteralDfa13_1(active0, 0x200000L);
            default:
                break;
        }
        return jjStartNfa_1(11, active0, 0L);
    }

    private int jjMoveStringLiteralDfa13_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(11, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(12, active0, 0L);
            return 13;
        }
        switch (curChar) {
            case 110:
                return jjMoveStringLiteralDfa14_1(active0, 0x200000L);
            default:
                break;
        }
        return jjStartNfa_1(12, active0, 0L);
    }

    private int jjMoveStringLiteralDfa14_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_1(12, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(13, active0, 0L);
            return 14;
        }
        switch (curChar) {
            case 115:
                if ((active0 & 0x200000L) != 0L) {
                    return jjStartNfaWithStates_1(14, 21, 1);
                }
                break;
            default:
                break;
        }
        return jjStartNfa_1(13, active0, 0L);
    }

    private int jjStartNfaWithStates_1(int pos, int kind, int state) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            return pos + 1;
        }
        return jjMoveNfa_1(state, pos + 1);
    }
    static final long[] jjbitVec0 = {
        0xffffffffffff0002L, 0x2ffffL, 0x0L, 0x0L
    };

    private int jjMoveNfa_1(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 3;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 2:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            if (kind > 61) {
                                kind = 61;
                            }
                             {
                                jjCheckNAdd(2);
                            }
                            break;
                        case 1:
                            if ((0x7ff000000000000L & l) == 0L) {
                                break;
                            }
                            if (kind > 22) {
                                kind = 22;
                            }
                            jjstateSet[jjnewStateCnt++] = 1;
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if ((0x7fffffe87fffffeL & l) == 0L) {
                                break;
                            }
                            if (kind > 22) {
                                kind = 22;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if (!jjCanMove_0(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 22) {
                                kind = 22;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 3 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private final int jjStopStringLiteralDfa_5(int pos, long active0) {
        switch (pos) {
            case 0:
                if ((active0 & 0x1800000000L) != 0L) {
                    jjmatchedKind = 37;
                    return 1;
                }
                return -1;
            case 1:
                if ((active0 & 0x1800000000L) != 0L) {
                    jjmatchedKind = 37;
                    jjmatchedPos = 1;
                    return 1;
                }
                return -1;
            case 2:
                if ((active0 & 0x1800000000L) != 0L) {
                    jjmatchedKind = 37;
                    jjmatchedPos = 2;
                    return 1;
                }
                return -1;
            case 3:
                if ((active0 & 0x1800000000L) != 0L) {
                    jjmatchedKind = 37;
                    jjmatchedPos = 3;
                    return 1;
                }
                return -1;
            case 4:
                if ((active0 & 0x1800000000L) != 0L) {
                    jjmatchedKind = 37;
                    jjmatchedPos = 4;
                    return 1;
                }
                return -1;
            case 5:
                if ((active0 & 0x1000000000L) != 0L) {
                    return 1;
                }
                if ((active0 & 0x800000000L) != 0L) {
                    jjmatchedKind = 37;
                    jjmatchedPos = 5;
                    return 1;
                }
                return -1;
            case 6:
                if ((active0 & 0x800000000L) != 0L) {
                    jjmatchedKind = 37;
                    jjmatchedPos = 6;
                    return 1;
                }
                return -1;
            case 7:
                if ((active0 & 0x800000000L) != 0L) {
                    jjmatchedKind = 37;
                    jjmatchedPos = 7;
                    return 1;
                }
                return -1;
            default:
                return -1;
        }
    }

    private final int jjStartNfa_5(int pos, long active0) {
        return jjMoveNfa_5(jjStopStringLiteralDfa_5(pos, active0), pos + 1);
    }

    private int jjMoveStringLiteralDfa0_5() {
        switch (curChar) {
            case 39:
                return jjStopAtPos(0, 41);
            case 44:
                return jjStopAtPos(0, 40);
            case 46:
                return jjStopAtPos(0, 38);
            case 61:
                return jjStopAtPos(0, 39);
            case 69:
                return jjMoveStringLiteralDfa1_5(0x800000000L);
            case 83:
                return jjMoveStringLiteralDfa1_5(0x1000000000L);
            default:
                return jjMoveNfa_5(0, 0);
        }
    }

    private int jjMoveStringLiteralDfa1_5(long active0) {
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_5(0, active0);
            return 1;
        }
        switch (curChar) {
            case 104:
                return jjMoveStringLiteralDfa2_5(active0, 0x1000000000L);
            case 120:
                return jjMoveStringLiteralDfa2_5(active0, 0x800000000L);
            default:
                break;
        }
        return jjStartNfa_5(0, active0);
    }

    private int jjMoveStringLiteralDfa2_5(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_5(0, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_5(1, active0);
            return 2;
        }
        switch (curChar) {
            case 97:
                return jjMoveStringLiteralDfa3_5(active0, 0x1000000000L);
            case 99:
                return jjMoveStringLiteralDfa3_5(active0, 0x800000000L);
            default:
                break;
        }
        return jjStartNfa_5(1, active0);
    }

    private int jjMoveStringLiteralDfa3_5(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_5(1, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_5(2, active0);
            return 3;
        }
        switch (curChar) {
            case 108:
                return jjMoveStringLiteralDfa4_5(active0, 0x800000000L);
            case 114:
                return jjMoveStringLiteralDfa4_5(active0, 0x1000000000L);
            default:
                break;
        }
        return jjStartNfa_5(2, active0);
    }

    private int jjMoveStringLiteralDfa4_5(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_5(2, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_5(3, active0);
            return 4;
        }
        switch (curChar) {
            case 101:
                return jjMoveStringLiteralDfa5_5(active0, 0x1000000000L);
            case 117:
                return jjMoveStringLiteralDfa5_5(active0, 0x800000000L);
            default:
                break;
        }
        return jjStartNfa_5(3, active0);
    }

    private int jjMoveStringLiteralDfa5_5(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_5(3, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_5(4, active0);
            return 5;
        }
        switch (curChar) {
            case 100:
                if ((active0 & 0x1000000000L) != 0L) {
                    return jjStartNfaWithStates_5(5, 36, 1);
                }
                break;
            case 115:
                return jjMoveStringLiteralDfa6_5(active0, 0x800000000L);
            default:
                break;
        }
        return jjStartNfa_5(4, active0);
    }

    private int jjMoveStringLiteralDfa6_5(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_5(4, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_5(5, active0);
            return 6;
        }
        switch (curChar) {
            case 105:
                return jjMoveStringLiteralDfa7_5(active0, 0x800000000L);
            default:
                break;
        }
        return jjStartNfa_5(5, active0);
    }

    private int jjMoveStringLiteralDfa7_5(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_5(5, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_5(6, active0);
            return 7;
        }
        switch (curChar) {
            case 118:
                return jjMoveStringLiteralDfa8_5(active0, 0x800000000L);
            default:
                break;
        }
        return jjStartNfa_5(6, active0);
    }

    private int jjMoveStringLiteralDfa8_5(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_5(6, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_5(7, active0);
            return 8;
        }
        switch (curChar) {
            case 101:
                if ((active0 & 0x800000000L) != 0L) {
                    return jjStartNfaWithStates_5(8, 35, 1);
                }
                break;
            default:
                break;
        }
        return jjStartNfa_5(7, active0);
    }

    private int jjStartNfaWithStates_5(int pos, int kind, int state) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            return pos + 1;
        }
        return jjMoveNfa_5(state, pos + 1);
    }

    private int jjMoveNfa_5(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 2;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 1:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            kind = 37;
                            jjstateSet[jjnewStateCnt++] = 1;
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if ((0x7fffffe87fffffeL & l) == 0L) {
                                break;
                            }
                            if (kind > 37) {
                                kind = 37;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if (!jjCanMove_0(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 37) {
                                kind = 37;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 2 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private final int jjStopStringLiteralDfa_4(int pos, long active0) {
        switch (pos) {
            case 0:
                if ((active0 & 0xc000000L) != 0L) {
                    jjmatchedKind = 28;
                    return 1;
                }
                if ((active0 & 0x8L) != 0L) {
                    return 2;
                }
                return -1;
            case 1:
                if ((active0 & 0xc000000L) != 0L) {
                    jjmatchedKind = 28;
                    jjmatchedPos = 1;
                    return 1;
                }
                return -1;
            case 2:
                if ((active0 & 0xc000000L) != 0L) {
                    jjmatchedKind = 28;
                    jjmatchedPos = 2;
                    return 1;
                }
                return -1;
            case 3:
                if ((active0 & 0xc000000L) != 0L) {
                    jjmatchedKind = 28;
                    jjmatchedPos = 3;
                    return 1;
                }
                return -1;
            case 4:
                if ((active0 & 0xc000000L) != 0L) {
                    jjmatchedKind = 28;
                    jjmatchedPos = 4;
                    return 1;
                }
                return -1;
            case 5:
                if ((active0 & 0x4000000L) != 0L) {
                    jjmatchedKind = 28;
                    jjmatchedPos = 5;
                    return 1;
                }
                if ((active0 & 0x8000000L) != 0L) {
                    return 1;
                }
                return -1;
            case 6:
                if ((active0 & 0x4000000L) != 0L) {
                    jjmatchedKind = 28;
                    jjmatchedPos = 6;
                    return 1;
                }
                return -1;
            case 7:
                if ((active0 & 0x4000000L) != 0L) {
                    jjmatchedKind = 28;
                    jjmatchedPos = 7;
                    return 1;
                }
                return -1;
            default:
                return -1;
        }
    }

    private final int jjStartNfa_4(int pos, long active0) {
        return jjMoveNfa_4(jjStopStringLiteralDfa_4(pos, active0), pos + 1);
    }

    private int jjMoveStringLiteralDfa0_4() {
        switch (curChar) {
            case 13:
                return jjStartNfaWithStates_4(0, 3, 2);
            case 44:
                return jjStopAtPos(0, 31);
            case 46:
                return jjStopAtPos(0, 29);
            case 61:
                return jjStopAtPos(0, 30);
            case 69:
                return jjMoveStringLiteralDfa1_4(0x4000000L);
            case 83:
                return jjMoveStringLiteralDfa1_4(0x8000000L);
            default:
                return jjMoveNfa_4(0, 0);
        }
    }

    private int jjMoveStringLiteralDfa1_4(long active0) {
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_4(0, active0);
            return 1;
        }
        switch (curChar) {
            case 104:
                return jjMoveStringLiteralDfa2_4(active0, 0x8000000L);
            case 120:
                return jjMoveStringLiteralDfa2_4(active0, 0x4000000L);
            default:
                break;
        }
        return jjStartNfa_4(0, active0);
    }

    private int jjMoveStringLiteralDfa2_4(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_4(0, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_4(1, active0);
            return 2;
        }
        switch (curChar) {
            case 97:
                return jjMoveStringLiteralDfa3_4(active0, 0x8000000L);
            case 99:
                return jjMoveStringLiteralDfa3_4(active0, 0x4000000L);
            default:
                break;
        }
        return jjStartNfa_4(1, active0);
    }

    private int jjMoveStringLiteralDfa3_4(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_4(1, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_4(2, active0);
            return 3;
        }
        switch (curChar) {
            case 108:
                return jjMoveStringLiteralDfa4_4(active0, 0x4000000L);
            case 114:
                return jjMoveStringLiteralDfa4_4(active0, 0x8000000L);
            default:
                break;
        }
        return jjStartNfa_4(2, active0);
    }

    private int jjMoveStringLiteralDfa4_4(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_4(2, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_4(3, active0);
            return 4;
        }
        switch (curChar) {
            case 101:
                return jjMoveStringLiteralDfa5_4(active0, 0x8000000L);
            case 117:
                return jjMoveStringLiteralDfa5_4(active0, 0x4000000L);
            default:
                break;
        }
        return jjStartNfa_4(3, active0);
    }

    private int jjMoveStringLiteralDfa5_4(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_4(3, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_4(4, active0);
            return 5;
        }
        switch (curChar) {
            case 100:
                if ((active0 & 0x8000000L) != 0L) {
                    return jjStartNfaWithStates_4(5, 27, 1);
                }
                break;
            case 115:
                return jjMoveStringLiteralDfa6_4(active0, 0x4000000L);
            default:
                break;
        }
        return jjStartNfa_4(4, active0);
    }

    private int jjMoveStringLiteralDfa6_4(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_4(4, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_4(5, active0);
            return 6;
        }
        switch (curChar) {
            case 105:
                return jjMoveStringLiteralDfa7_4(active0, 0x4000000L);
            default:
                break;
        }
        return jjStartNfa_4(5, active0);
    }

    private int jjMoveStringLiteralDfa7_4(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_4(5, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_4(6, active0);
            return 7;
        }
        switch (curChar) {
            case 118:
                return jjMoveStringLiteralDfa8_4(active0, 0x4000000L);
            default:
                break;
        }
        return jjStartNfa_4(6, active0);
    }

    private int jjMoveStringLiteralDfa8_4(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_4(6, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_4(7, active0);
            return 8;
        }
        switch (curChar) {
            case 101:
                if ((active0 & 0x4000000L) != 0L) {
                    return jjStartNfaWithStates_4(8, 26, 1);
                }
                break;
            default:
                break;
        }
        return jjStartNfa_4(7, active0);
    }

    private int jjStartNfaWithStates_4(int pos, int kind, int state) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            return pos + 1;
        }
        return jjMoveNfa_4(state, pos + 1);
    }

    private int jjMoveNfa_4(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 5;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                            if ((0x2400L & l) != 0L) {
                                if (kind > 32) {
                                    kind = 32;
                                }
                            }
                            if (curChar == 13) {
                                jjstateSet[jjnewStateCnt++] = 2;
                            }
                            break;
                        case 1:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            kind = 28;
                            jjstateSet[jjnewStateCnt++] = 1;
                            break;
                        case 2:
                            if (curChar == 10 && kind > 32) {
                                kind = 32;
                            }
                            break;
                        case 3:
                            if (curChar == 13) {
                                jjstateSet[jjnewStateCnt++] = 2;
                            }
                            break;
                        case 4:
                            if ((0x2400L & l) != 0L && kind > 32) {
                                kind = 32;
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if ((0x7fffffe87fffffeL & l) == 0L) {
                                break;
                            }
                            if (kind > 28) {
                                kind = 28;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if (!jjCanMove_0(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 28) {
                                kind = 28;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 5 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private final int jjStopStringLiteralDfa_0(int pos, long active0, long active1) {
        switch (pos) {
            case 0:
                if ((active0 & 0x7ff0L) != 0L) {
                    jjmatchedKind = 15;
                    return 1;
                }
                return -1;
            case 1:
                if ((active0 & 0x7ff0L) != 0L) {
                    jjmatchedKind = 15;
                    jjmatchedPos = 1;
                    return 1;
                }
                return -1;
            case 2:
                if ((active0 & 0x7fd0L) != 0L) {
                    jjmatchedKind = 15;
                    jjmatchedPos = 2;
                    return 1;
                }
                if ((active0 & 0x20L) != 0L) {
                    return 1;
                }
                return -1;
            case 3:
                if ((active0 & 0x6bd0L) != 0L) {
                    jjmatchedKind = 15;
                    jjmatchedPos = 3;
                    return 1;
                }
                if ((active0 & 0x1400L) != 0L) {
                    return 1;
                }
                return -1;
            case 4:
                if ((active0 & 0x4800L) != 0L) {
                    return 1;
                }
                if ((active0 & 0x23d0L) != 0L) {
                    jjmatchedKind = 15;
                    jjmatchedPos = 4;
                    return 1;
                }
                return -1;
            case 5:
                if ((active0 & 0x23d0L) != 0L) {
                    jjmatchedKind = 15;
                    jjmatchedPos = 5;
                    return 1;
                }
                return -1;
            case 6:
                if ((active0 & 0x50L) != 0L) {
                    return 1;
                }
                if ((active0 & 0x2380L) != 0L) {
                    jjmatchedKind = 15;
                    jjmatchedPos = 6;
                    return 1;
                }
                return -1;
            case 7:
                if ((active0 & 0x80L) != 0L) {
                    return 1;
                }
                if ((active0 & 0x2300L) != 0L) {
                    jjmatchedKind = 15;
                    jjmatchedPos = 7;
                    return 1;
                }
                return -1;
            default:
                return -1;
        }
    }

    private final int jjStartNfa_0(int pos, long active0, long active1) {
        return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0, active1), pos + 1);
    }

    private int jjMoveStringLiteralDfa0_0() {
        switch (curChar) {
            case 10:
                return jjStopAtPos(0, 55);
            case 44:
                return jjStopAtPos(0, 59);
            case 45:
                return jjStopAtPos(0, 69);
            case 67:
                return jjMoveStringLiteralDfa1_0(0x10L);
            case 68:
                return jjMoveStringLiteralDfa1_0(0x3e0L);
            case 69:
                return jjMoveStringLiteralDfa1_0(0x400L);
            case 83:
                return jjMoveStringLiteralDfa1_0(0x1800L);
            case 84:
                return jjMoveStringLiteralDfa1_0(0x6000L);
            case 65279:
                return jjStopAtPos(0, 56);
            default:
                return jjMoveNfa_0(0, 0);
        }
    }

    private int jjMoveStringLiteralDfa1_0(long active0) {
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(0, active0, 0L);
            return 1;
        }
        switch (curChar) {
            case 66:
                return jjMoveStringLiteralDfa2_0(active0, 0x3e0L);
            case 67:
                return jjMoveStringLiteralDfa2_0(active0, 0x800L);
            case 68:
                return jjMoveStringLiteralDfa2_0(active0, 0x3000L);
            case 76:
                return jjMoveStringLiteralDfa2_0(active0, 0x4000L);
            case 88:
                return jjMoveStringLiteralDfa2_0(active0, 0x400L);
            case 111:
                return jjMoveStringLiteralDfa2_0(active0, 0x10L);
            default:
                break;
        }
        return jjStartNfa_0(0, active0, 0L);
    }

    private int jjMoveStringLiteralDfa2_0(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_0(0, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(1, active0, 0L);
            return 2;
        }
        switch (curChar) {
            case 50:
                if ((active0 & 0x20L) != 0L) {
                    return jjStartNfaWithStates_0(2, 5, 1);
                }
                break;
            case 65:
                return jjMoveStringLiteralDfa3_0(active0, 0x800L);
            case 66:
                return jjMoveStringLiteralDfa3_0(active0, 0x1000L);
            case 67:
                return jjMoveStringLiteralDfa3_0(active0, 0x400L);
            case 69:
                return jjMoveStringLiteralDfa3_0(active0, 0x2000L);
            case 77:
                return jjMoveStringLiteralDfa3_0(active0, 0x40L);
            case 79:
                return jjMoveStringLiteralDfa3_0(active0, 0x4080L);
            case 80:
                return jjMoveStringLiteralDfa3_0(active0, 0x100L);
            case 86:
                return jjMoveStringLiteralDfa3_0(active0, 0x200L);
            case 110:
                return jjMoveStringLiteralDfa3_0(active0, 0x10L);
            default:
                break;
        }
        return jjStartNfa_0(1, active0, 0L);
    }

    private int jjMoveStringLiteralDfa3_0(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_0(1, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(2, active0, 0L);
            return 3;
        }
        switch (curChar) {
            case 56:
                return jjMoveStringLiteralDfa4_0(active0, 0x200L);
            case 65:
                return jjMoveStringLiteralDfa4_0(active0, 0x2000L);
            case 67:
                return jjMoveStringLiteralDfa4_0(active0, 0x4000L);
            case 76:
                if ((active0 & 0x1000L) != 0L) {
                    return jjStartNfaWithStates_0(3, 12, 1);
                }
                return jjMoveStringLiteralDfa4_0(active0, 0x800L);
            case 79:
                return jjMoveStringLiteralDfa4_0(active0, 0x100L);
            case 80:
                if ((active0 & 0x400L) != 0L) {
                    return jjStartNfaWithStates_0(3, 10, 1);
                }
                break;
            case 82:
                return jjMoveStringLiteralDfa4_0(active0, 0x80L);
            case 83:
                return jjMoveStringLiteralDfa4_0(active0, 0x40L);
            case 116:
                return jjMoveStringLiteralDfa4_0(active0, 0x10L);
            default:
                break;
        }
        return jjStartNfa_0(2, active0, 0L);
    }

    private int jjMoveStringLiteralDfa4_0(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_0(2, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(3, active0, 0L);
            return 4;
        }
        switch (curChar) {
            case 65:
                return jjMoveStringLiteralDfa5_0(active0, 0x80L);
            case 68:
                return jjMoveStringLiteralDfa5_0(active0, 0x2200L);
            case 75:
                if ((active0 & 0x4000L) != 0L) {
                    return jjStartNfaWithStates_0(4, 14, 1);
                }
                break;
            case 76:
                if ((active0 & 0x800L) != 0L) {
                    return jjStartNfaWithStates_0(4, 11, 1);
                }
                break;
            case 83:
                return jjMoveStringLiteralDfa5_0(active0, 0x140L);
            case 101:
                return jjMoveStringLiteralDfa5_0(active0, 0x10L);
            default:
                break;
        }
        return jjStartNfa_0(3, active0, 0L);
    }

    private int jjMoveStringLiteralDfa5_0(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_0(3, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(4, active0, 0L);
            return 5;
        }
        switch (curChar) {
            case 66:
                return jjMoveStringLiteralDfa6_0(active0, 0x200L);
            case 67:
                return jjMoveStringLiteralDfa6_0(active0, 0x80L);
            case 76:
                return jjMoveStringLiteralDfa6_0(active0, 0x2000L);
            case 81:
                return jjMoveStringLiteralDfa6_0(active0, 0x40L);
            case 84:
                return jjMoveStringLiteralDfa6_0(active0, 0x100L);
            case 120:
                return jjMoveStringLiteralDfa6_0(active0, 0x10L);
            default:
                break;
        }
        return jjStartNfa_0(4, active0, 0L);
    }

    private int jjMoveStringLiteralDfa6_0(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_0(4, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(5, active0, 0L);
            return 6;
        }
        switch (curChar) {
            case 69:
                return jjMoveStringLiteralDfa7_0(active0, 0x200L);
            case 71:
                return jjMoveStringLiteralDfa7_0(active0, 0x100L);
            case 76:
                if ((active0 & 0x40L) != 0L) {
                    return jjStartNfaWithStates_0(6, 6, 1);
                }
                return jjMoveStringLiteralDfa7_0(active0, 0x80L);
            case 79:
                return jjMoveStringLiteralDfa7_0(active0, 0x2000L);
            case 116:
                if ((active0 & 0x10L) != 0L) {
                    return jjStartNfaWithStates_0(6, 4, 1);
                }
                break;
            default:
                break;
        }
        return jjStartNfa_0(5, active0, 0L);
    }

    private int jjMoveStringLiteralDfa7_0(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_0(5, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(6, active0, 0L);
            return 7;
        }
        switch (curChar) {
            case 67:
                return jjMoveStringLiteralDfa8_0(active0, 0x2000L);
            case 69:
                if ((active0 & 0x80L) != 0L) {
                    return jjStartNfaWithStates_0(7, 7, 1);
                }
                break;
            case 82:
                return jjMoveStringLiteralDfa8_0(active0, 0x100L);
            case 110:
                return jjMoveStringLiteralDfa8_0(active0, 0x200L);
            default:
                break;
        }
        return jjStartNfa_0(6, active0, 0L);
    }

    private int jjMoveStringLiteralDfa8_0(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_0(6, old0, 0L);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(7, active0, 0L);
            return 8;
        }
        switch (curChar) {
            case 75:
                if ((active0 & 0x2000L) != 0L) {
                    return jjStartNfaWithStates_0(8, 13, 1);
                }
                break;
            case 83:
                if ((active0 & 0x100L) != 0L) {
                    return jjStartNfaWithStates_0(8, 8, 1);
                }
                break;
            case 103:
                if ((active0 & 0x200L) != 0L) {
                    return jjStartNfaWithStates_0(8, 9, 1);
                }
                break;
            default:
                break;
        }
        return jjStartNfa_0(7, active0, 0L);
    }

    private int jjStartNfaWithStates_0(int pos, int kind, int state) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            return pos + 1;
        }
        return jjMoveNfa_0(state, pos + 1);
    }

    private int jjMoveNfa_0(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 15;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            if (kind > 58) {
                                kind = 58;
                            }
                             {
                                jjCheckNAddTwoStates(3, 14);
                            }
                            break;
                        case 1:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            if (kind > 15) {
                                kind = 15;
                            }
                            jjstateSet[jjnewStateCnt++] = 1;
                            break;
                        case 3:
                            if ((0x3ff000000000000L & l) != 0L) {
                                jjstateSet[jjnewStateCnt++] = 4;
                            }
                            break;
                        case 4:
                            if (curChar == 58) {
                                jjstateSet[jjnewStateCnt++] = 5;
                            }
                            break;
                        case 5:
                            if ((0x3ff000000000000L & l) != 0L) {
                                jjstateSet[jjnewStateCnt++] = 6;
                            }
                            break;
                        case 6:
                            if ((0x3ff000000000000L & l) != 0L) {
                                jjstateSet[jjnewStateCnt++] = 7;
                            }
                            break;
                        case 7:
                            if (curChar == 46) {
                                jjstateSet[jjnewStateCnt++] = 8;
                            }
                            break;
                        case 8:
                            if ((0x3ff000000000000L & l) != 0L) {
                                jjstateSet[jjnewStateCnt++] = 9;
                            }
                            break;
                        case 9:
                            if ((0x3ff000000000000L & l) != 0L) {
                                jjstateSet[jjnewStateCnt++] = 10;
                            }
                            break;
                        case 10:
                            if ((0x3ff000000000000L & l) != 0L) {
                                jjstateSet[jjnewStateCnt++] = 11;
                            }
                            break;
                        case 11:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            if (kind > 57) {
                                kind = 57;
                            }
                            jjstateSet[jjnewStateCnt++] = 12;
                            break;
                        case 12:
                            if ((0x3ff000000000000L & l) != 0L) {
                                jjstateSet[jjnewStateCnt++] = 13;
                            }
                            break;
                        case 13:
                            if ((0x3ff000000000000L & l) != 0L && kind > 57) {
                                kind = 57;
                            }
                            break;
                        case 14:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            if (kind > 58) {
                                kind = 58;
                            }
                             {
                                jjCheckNAdd(14);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if ((0x7fffffe87fffffeL & l) == 0L) {
                                break;
                            }
                            if (kind > 15) {
                                kind = 15;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if (!jjCanMove_0(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 15) {
                                kind = 15;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 15 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private final int jjStopStringLiteralDfa_2(int pos, long active0, long active1) {
        switch (pos) {
            default:
                return -1;
        }
    }

    private final int jjStartNfa_2(int pos, long active0, long active1) {
        return jjMoveNfa_2(jjStopStringLiteralDfa_2(pos, active0, active1), pos + 1);
    }

    private int jjMoveStringLiteralDfa0_2() {
        switch (curChar) {
            case 13:
                return jjStartNfaWithStates_2(0, 3, 0);
            case 32:
                return jjStartNfaWithStates_2(0, 1, 15);
            case 44:
                return jjStopAtPos(0, 68);
            default:
                return jjMoveNfa_2(1, 0);
        }
    }

    private int jjStartNfaWithStates_2(int pos, int kind, int state) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            return pos + 1;
        }
        return jjMoveNfa_2(state, pos + 1);
    }
    static final long[] jjbitVec1 = {
        0xfffffffffffffffeL, 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL
    };
    static final long[] jjbitVec3 = {
        0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL
    };

    private int jjMoveNfa_2(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 16;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 1:
                            if ((0xffffef7bffffd9ffL & l) != 0L) {
                                if (kind > 67) {
                                    kind = 67;
                                }
                                {
                                    jjCheckNAdd(15);
                                }
                            }
                            else if ((0x2400L & l) != 0L) {
                                if (kind > 64) {
                                    kind = 64;
                                }
                            }
                            else if (curChar == 39) {
                                jjCheckNAddStates(0, 2);
                            }
                            else if (curChar == 34) {
                                jjCheckNAddStates(3, 5);
                            }
                            if ((0x3ff000000000000L & l) != 0L) {
                                if (kind > 65) {
                                    kind = 65;
                                }
                                {
                                    jjCheckNAdd(3);
                                }
                            }
                            else if (curChar == 13) {
                                jjstateSet[jjnewStateCnt++] = 0;
                            }
                            break;
                        case 0:
                            if (curChar == 10 && kind > 64) {
                                kind = 64;
                            }
                            break;
                        case 2:
                            if ((0x2400L & l) != 0L && kind > 64) {
                                kind = 64;
                            }
                            break;
                        case 3:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            if (kind > 65) {
                                kind = 65;
                            }
                             {
                                jjCheckNAdd(3);
                            }
                            break;
                        case 4:
                        case 6:
                            if (curChar == 34) {
                                jjCheckNAddStates(3, 5);
                            }
                            break;
                        case 5:
                            if ((0xfffffffbffffffffL & l) != 0L) {
                                jjCheckNAddStates(3, 5);
                            }
                            break;
                        case 7:
                            if (curChar == 34) {
                                jjstateSet[jjnewStateCnt++] = 6;
                            }
                            break;
                        case 8:
                            if (curChar == 34 && kind > 66) {
                                kind = 66;
                            }
                            break;
                        case 9:
                        case 11:
                            if (curChar == 39) {
                                jjCheckNAddStates(0, 2);
                            }
                            break;
                        case 10:
                            if ((0xffffff7fffffffffL & l) != 0L) {
                                jjCheckNAddStates(0, 2);
                            }
                            break;
                        case 12:
                            if (curChar == 39) {
                                jjstateSet[jjnewStateCnt++] = 11;
                            }
                            break;
                        case 13:
                            if (curChar == 39 && kind > 66) {
                                kind = 66;
                            }
                            break;
                        case 14:
                            if ((0xffffef7bffffd9ffL & l) == 0L) {
                                break;
                            }
                            if (kind > 67) {
                                kind = 67;
                            }
                             {
                                jjCheckNAdd(15);
                            }
                            break;
                        case 15:
                            if ((0xffffefffffffd9ffL & l) == 0L) {
                                break;
                            }
                            if (kind > 67) {
                                kind = 67;
                            }
                             {
                                jjCheckNAdd(15);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 1:
                        case 15:
                            if (kind > 67) {
                                kind = 67;
                            }
                             {
                                jjCheckNAdd(15);
                            }
                            break;
                        case 5: {
                            jjAddStates(3, 5);
                        }
                        break;
                        case 10: {
                            jjAddStates(0, 2);
                        }
                        break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 1:
                        case 15:
                            if (!jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 67) {
                                kind = 67;
                            }
                             {
                                jjCheckNAdd(15);
                            }
                            break;
                        case 5:
                            if (jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                jjAddStates(3, 5);
                            }
                            break;
                        case 10:
                            if (jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                jjAddStates(0, 2);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 16 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private int jjMoveStringLiteralDfa0_7() {
        return jjMoveNfa_7(0, 0);
    }

    private int jjMoveNfa_7(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 12;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                            if ((0xffffef7bffffffffL & l) != 0L) {
                                if (kind > 34) {
                                    kind = 34;
                                }
                                {
                                    jjCheckNAdd(11);
                                }
                            }
                            else if (curChar == 39) {
                                jjCheckNAddStates(6, 8);
                            }
                            else if (curChar == 34) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 1:
                            if ((0xfffffffbffffffffL & l) != 0L) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 2:
                            if (curChar == 34) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 3:
                            if (curChar == 34) {
                                jjstateSet[jjnewStateCnt++] = 2;
                            }
                            break;
                        case 4:
                            if (curChar == 34 && kind > 33) {
                                kind = 33;
                            }
                            break;
                        case 5:
                        case 7:
                            if (curChar == 39) {
                                jjCheckNAddStates(6, 8);
                            }
                            break;
                        case 6:
                            if ((0xffffff7fffffffffL & l) != 0L) {
                                jjCheckNAddStates(6, 8);
                            }
                            break;
                        case 8:
                            if (curChar == 39) {
                                jjstateSet[jjnewStateCnt++] = 7;
                            }
                            break;
                        case 9:
                            if (curChar == 39 && kind > 33) {
                                kind = 33;
                            }
                            break;
                        case 10:
                            if ((0xffffef7bffffffffL & l) == 0L) {
                                break;
                            }
                            if (kind > 34) {
                                kind = 34;
                            }
                             {
                                jjCheckNAdd(11);
                            }
                            break;
                        case 11:
                            if ((0xffffeffeffffd9ffL & l) == 0L) {
                                break;
                            }
                            if (kind > 34) {
                                kind = 34;
                            }
                             {
                                jjCheckNAdd(11);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 11:
                            if (kind > 34) {
                                kind = 34;
                            }
                             {
                                jjCheckNAdd(11);
                            }
                            break;
                        case 1: {
                            jjAddStates(9, 11);
                        }
                        break;
                        case 6: {
                            jjAddStates(6, 8);
                        }
                        break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 11:
                            if (!jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 34) {
                                kind = 34;
                            }
                             {
                                jjCheckNAdd(11);
                            }
                            break;
                        case 1:
                            if (jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                jjAddStates(9, 11);
                            }
                            break;
                        case 6:
                            if (jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                jjAddStates(6, 8);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 12 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private int jjMoveStringLiteralDfa0_3() {
        switch (curChar) {
            case 61:
                jjmatchedKind = 25;
                return jjMoveStringLiteralDfa1_3(0x1800000L);
            default:
                return 1;
        }
    }

    private int jjMoveStringLiteralDfa1_3(long active0) {
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            return 1;
        }
        switch (curChar) {
            case 34:
                if ((active0 & 0x1000000L) != 0L) {
                    return jjStopAtPos(1, 24);
                }
                break;
            case 39:
                if ((active0 & 0x800000L) != 0L) {
                    return jjStopAtPos(1, 23);
                }
                break;
            default:
                return 2;
        }
        return 2;
    }

    private int jjMoveStringLiteralDfa0_9() {
        return jjMoveNfa_9(0, 0);
    }

    private int jjMoveNfa_9(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 7;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                            if ((0xffffeffbffffffffL & l) != 0L) {
                                if (kind > 52) {
                                    kind = 52;
                                }
                                {
                                    jjCheckNAdd(6);
                                }
                            }
                            if (curChar == 39) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 1:
                            if ((0xffffff7fffffffffL & l) != 0L) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 2:
                            if (curChar == 39) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 3:
                            if (curChar == 39) {
                                jjstateSet[jjnewStateCnt++] = 2;
                            }
                            break;
                        case 4:
                            if (curChar == 39 && kind > 51) {
                                kind = 51;
                            }
                            break;
                        case 5:
                            if ((0xffffeffbffffffffL & l) == 0L) {
                                break;
                            }
                            if (kind > 52) {
                                kind = 52;
                            }
                             {
                                jjCheckNAdd(6);
                            }
                            break;
                        case 6:
                            if ((0xffffeffaffffd9ffL & l) == 0L) {
                                break;
                            }
                            if (kind > 52) {
                                kind = 52;
                            }
                             {
                                jjCheckNAdd(6);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 6:
                            if (kind > 52) {
                                kind = 52;
                            }
                             {
                                jjCheckNAdd(6);
                            }
                            break;
                        case 1: {
                            jjAddStates(9, 11);
                        }
                        break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 6:
                            if (!jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 52) {
                                kind = 52;
                            }
                             {
                                jjCheckNAdd(6);
                            }
                            break;
                        case 1:
                            if (jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                jjAddStates(9, 11);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 7 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private int jjMoveStringLiteralDfa0_8() {
        return jjMoveNfa_8(0, 0);
    }

    private int jjMoveNfa_8(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 7;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                            if ((0xffffef7fffffffffL & l) != 0L) {
                                if (kind > 43) {
                                    kind = 43;
                                }
                                {
                                    jjCheckNAdd(6);
                                }
                            }
                            if (curChar == 34) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 1:
                            if ((0xfffffffbffffffffL & l) != 0L) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 2:
                            if (curChar == 34) {
                                jjCheckNAddStates(9, 11);
                            }
                            break;
                        case 3:
                            if (curChar == 34) {
                                jjstateSet[jjnewStateCnt++] = 2;
                            }
                            break;
                        case 4:
                            if (curChar == 34 && kind > 42) {
                                kind = 42;
                            }
                            break;
                        case 5:
                            if ((0xffffef7fffffffffL & l) == 0L) {
                                break;
                            }
                            if (kind > 43) {
                                kind = 43;
                            }
                             {
                                jjCheckNAdd(6);
                            }
                            break;
                        case 6:
                            if ((0xffffef7effffd9ffL & l) == 0L) {
                                break;
                            }
                            if (kind > 43) {
                                kind = 43;
                            }
                             {
                                jjCheckNAdd(6);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 6:
                            if (kind > 43) {
                                kind = 43;
                            }
                             {
                                jjCheckNAdd(6);
                            }
                            break;
                        case 1: {
                            jjAddStates(9, 11);
                        }
                        break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 6:
                            if (!jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 43) {
                                kind = 43;
                            }
                             {
                                jjCheckNAdd(6);
                            }
                            break;
                        case 1:
                            if (jjCanMove_1(hiByte, i1, i2, l1, l2)) {
                                jjAddStates(9, 11);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 7 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private final int jjStopStringLiteralDfa_6(int pos, long active0) {
        switch (pos) {
            case 0:
                if ((active0 & 0x300000000000L) != 0L) {
                    jjmatchedKind = 46;
                    return 1;
                }
                return -1;
            case 1:
                if ((active0 & 0x300000000000L) != 0L) {
                    jjmatchedKind = 46;
                    jjmatchedPos = 1;
                    return 1;
                }
                return -1;
            case 2:
                if ((active0 & 0x300000000000L) != 0L) {
                    jjmatchedKind = 46;
                    jjmatchedPos = 2;
                    return 1;
                }
                return -1;
            case 3:
                if ((active0 & 0x300000000000L) != 0L) {
                    jjmatchedKind = 46;
                    jjmatchedPos = 3;
                    return 1;
                }
                return -1;
            case 4:
                if ((active0 & 0x300000000000L) != 0L) {
                    jjmatchedKind = 46;
                    jjmatchedPos = 4;
                    return 1;
                }
                return -1;
            case 5:
                if ((active0 & 0x100000000000L) != 0L) {
                    jjmatchedKind = 46;
                    jjmatchedPos = 5;
                    return 1;
                }
                if ((active0 & 0x200000000000L) != 0L) {
                    return 1;
                }
                return -1;
            case 6:
                if ((active0 & 0x100000000000L) != 0L) {
                    jjmatchedKind = 46;
                    jjmatchedPos = 6;
                    return 1;
                }
                return -1;
            case 7:
                if ((active0 & 0x100000000000L) != 0L) {
                    jjmatchedKind = 46;
                    jjmatchedPos = 7;
                    return 1;
                }
                return -1;
            default:
                return -1;
        }
    }

    private final int jjStartNfa_6(int pos, long active0) {
        return jjMoveNfa_6(jjStopStringLiteralDfa_6(pos, active0), pos + 1);
    }

    private int jjMoveStringLiteralDfa0_6() {
        switch (curChar) {
            case 34:
                return jjStopAtPos(0, 50);
            case 44:
                return jjStopAtPos(0, 49);
            case 46:
                return jjStopAtPos(0, 47);
            case 61:
                return jjStopAtPos(0, 48);
            case 69:
                return jjMoveStringLiteralDfa1_6(0x100000000000L);
            case 83:
                return jjMoveStringLiteralDfa1_6(0x200000000000L);
            default:
                return jjMoveNfa_6(0, 0);
        }
    }

    private int jjMoveStringLiteralDfa1_6(long active0) {
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_6(0, active0);
            return 1;
        }
        switch (curChar) {
            case 104:
                return jjMoveStringLiteralDfa2_6(active0, 0x200000000000L);
            case 120:
                return jjMoveStringLiteralDfa2_6(active0, 0x100000000000L);
            default:
                break;
        }
        return jjStartNfa_6(0, active0);
    }

    private int jjMoveStringLiteralDfa2_6(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_6(0, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_6(1, active0);
            return 2;
        }
        switch (curChar) {
            case 97:
                return jjMoveStringLiteralDfa3_6(active0, 0x200000000000L);
            case 99:
                return jjMoveStringLiteralDfa3_6(active0, 0x100000000000L);
            default:
                break;
        }
        return jjStartNfa_6(1, active0);
    }

    private int jjMoveStringLiteralDfa3_6(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_6(1, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_6(2, active0);
            return 3;
        }
        switch (curChar) {
            case 108:
                return jjMoveStringLiteralDfa4_6(active0, 0x100000000000L);
            case 114:
                return jjMoveStringLiteralDfa4_6(active0, 0x200000000000L);
            default:
                break;
        }
        return jjStartNfa_6(2, active0);
    }

    private int jjMoveStringLiteralDfa4_6(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_6(2, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_6(3, active0);
            return 4;
        }
        switch (curChar) {
            case 101:
                return jjMoveStringLiteralDfa5_6(active0, 0x200000000000L);
            case 117:
                return jjMoveStringLiteralDfa5_6(active0, 0x100000000000L);
            default:
                break;
        }
        return jjStartNfa_6(3, active0);
    }

    private int jjMoveStringLiteralDfa5_6(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_6(3, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_6(4, active0);
            return 5;
        }
        switch (curChar) {
            case 100:
                if ((active0 & 0x200000000000L) != 0L) {
                    return jjStartNfaWithStates_6(5, 45, 1);
                }
                break;
            case 115:
                return jjMoveStringLiteralDfa6_6(active0, 0x100000000000L);
            default:
                break;
        }
        return jjStartNfa_6(4, active0);
    }

    private int jjMoveStringLiteralDfa6_6(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_6(4, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_6(5, active0);
            return 6;
        }
        switch (curChar) {
            case 105:
                return jjMoveStringLiteralDfa7_6(active0, 0x100000000000L);
            default:
                break;
        }
        return jjStartNfa_6(5, active0);
    }

    private int jjMoveStringLiteralDfa7_6(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_6(5, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_6(6, active0);
            return 7;
        }
        switch (curChar) {
            case 118:
                return jjMoveStringLiteralDfa8_6(active0, 0x100000000000L);
            default:
                break;
        }
        return jjStartNfa_6(6, active0);
    }

    private int jjMoveStringLiteralDfa8_6(long old0, long active0) {
        if (((active0 &= old0)) == 0L) {
            return jjStartNfa_6(6, old0);
        }
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            jjStopStringLiteralDfa_6(7, active0);
            return 8;
        }
        switch (curChar) {
            case 101:
                if ((active0 & 0x100000000000L) != 0L) {
                    return jjStartNfaWithStates_6(8, 44, 1);
                }
                break;
            default:
                break;
        }
        return jjStartNfa_6(7, active0);
    }

    private int jjStartNfaWithStates_6(int pos, int kind, int state) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        try {
            curChar = input_stream.readChar();
        }
        catch (java.io.IOException e) {
            return pos + 1;
        }
        return jjMoveNfa_6(state, pos + 1);
    }

    private int jjMoveNfa_6(int startState, int curPos) {
        int startsAt = 0;
        jjnewStateCnt = 2;
        int i = 1;
        jjstateSet[0] = startState;
        int kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff) {
                ReInitRounds();
            }
            if (curChar < 64) {
                long l = 1L << curChar;
                do {
                    switch (jjstateSet[--i]) {
                        case 1:
                            if ((0x3ff000000000000L & l) == 0L) {
                                break;
                            }
                            kind = 46;
                            jjstateSet[jjnewStateCnt++] = 1;
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if ((0x7fffffe87fffffeL & l) == 0L) {
                                break;
                            }
                            if (kind > 46) {
                                kind = 46;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            break;
                    }
                }
                while (i != startsAt);
            }
            else {
                int hiByte = (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                do {
                    switch (jjstateSet[--i]) {
                        case 0:
                        case 1:
                            if (!jjCanMove_0(hiByte, i1, i2, l1, l2)) {
                                break;
                            }
                            if (kind > 46) {
                                kind = 46;
                            }
                             {
                                jjCheckNAdd(1);
                            }
                            break;
                        default:
                            if (i1 == 0 || l1 == 0 || i2 == 0 || l2 == 0) {
                                break;
                            }
                            else {
                                break;
                            }
                    }
                }
                while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 2 - (jjnewStateCnt = startsAt))) {
                return curPos;
            }
            try {
                curChar = input_stream.readChar();
            }
            catch (java.io.IOException e) {
                return curPos;
            }
        }
    }
    static final int[] jjnextStates = {
        10, 12, 13, 5, 7, 8, 6, 8, 9, 1, 3, 4,};

    private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2) {
        switch (hiByte) {
            case 4:
                return ((jjbitVec0[i2] & l2) != 0L);
            default:
                return false;
        }
    }

    private static final boolean jjCanMove_1(int hiByte, int i1, int i2, long l1, long l2) {
        switch (hiByte) {
            case 0:
                return ((jjbitVec3[i2] & l2) != 0L);
            default:
                if ((jjbitVec1[i1] & l1) != 0L) {
                    return true;
                }
                return false;
        }
    }

    /**
     * Token literal values.
     */
    public static final String[] jjstrLiteralImages = {
        "", null, null, null, "\103\157\156\164\145\170\164", "\104\102\62",
        "\104\102\115\123\123\121\114", "\104\102\117\122\101\103\114\105", "\104\102\120\117\123\124\107\122\123",
        "\104\102\126\70\104\102\105\156\147", "\105\130\103\120", "\123\103\101\114\114", "\123\104\102\114",
        "\124\104\105\101\104\114\117\103\113", "\124\114\117\103\113", null, "\103\157\156\164\145\170\164",
        "\145\163\143\141\154\141\164\151\156\147", "\154\153\163\162\143", "\114\157\143\153\163", "\123\161\154",
        "\127\141\151\164\103\157\156\156\145\143\164\151\157\156\163", null, "\75\47", "\75\42", "\75", "\105\170\143\154\165\163\151\166\145",
        "\123\150\141\162\145\144", null, "\56", "\75", "\54", null, null, null,
        "\105\170\143\154\165\163\151\166\145", "\123\150\141\162\145\144", null, "\56", "\75", "\54", "\47", null, null,
        "\105\170\143\154\165\163\151\166\145", "\123\150\141\162\145\144", null, "\56", "\75", "\54", "\42", null, null, null,
        null, "\12", "\ufeff", null, null, "\54", "\12", null, "\75", "\54", null, null,
        null, null, "\54", "\55",};

    protected Token jjFillToken() {
        final Token t;
        final String curTokenImage;
        final int beginLine;
        final int endLine;
        final int beginColumn;
        final int endColumn;
        String im = jjstrLiteralImages[jjmatchedKind];
        curTokenImage = (im == null) ? input_stream.GetImage() : im;
        beginLine = input_stream.getBeginLine();
        beginColumn = input_stream.getBeginColumn();
        endLine = input_stream.getEndLine();
        endColumn = input_stream.getEndColumn();
        t = Token.newToken(jjmatchedKind, curTokenImage);

        t.beginLine = beginLine;
        t.endLine = endLine;
        t.beginColumn = beginColumn;
        t.endColumn = endColumn;
        t.bytesRead = input_stream.bytesRead;

        return t;
    }

    int curLexState = 0;
    int defaultLexState = 0;
    int jjnewStateCnt;
    int jjround;
    int jjmatchedPos;
    int jjmatchedKind;

    /**
     * Get the next Token.
     */
    public Token getNextToken() {
        Token matchedToken;
        int curPos = 0;

        EOFLoop:
        for (;;) {
            try {
                curChar = input_stream.BeginToken();
            }
            catch (Exception e) {
                jjmatchedKind = 0;
                jjmatchedPos = -1;
                matchedToken = jjFillToken();
                return matchedToken;
            }

            switch (curLexState) {

                case 0:
                try {
                    input_stream.backup(0);
                    while (curChar <= 32 && (0x100002200L & (1L << curChar)) != 0L) {
                        curChar = input_stream.BeginToken();
                    }
                }
                catch (java.io.IOException e1) {
                    continue EOFLoop;
                }
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_0();
                break;

                case 1:
                try {
                    input_stream.backup(0);
                    while (curChar <= 32 && (0x100002200L & (1L << curChar)) != 0L) {
                        curChar = input_stream.BeginToken();
                    }
                }
                catch (java.io.IOException e1) {
                    continue EOFLoop;
                }
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_1();
                break;
                
                case 2:
                try {
                    input_stream.backup(0);
                    while (curChar <= 9 && (0x200L & (1L << curChar)) != 0L) {
                        curChar = input_stream.BeginToken();
                    }
                }
                catch (java.io.IOException e1) {
                    continue EOFLoop;
                }
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_2();
                break;
                
                case 3:
                try {
                    input_stream.backup(0);
                    while (curChar <= 32 && (0x100002200L & (1L << curChar)) != 0L) {
                        curChar = input_stream.BeginToken();
                    }
                }
                catch (java.io.IOException e1) {
                    continue EOFLoop;
                }
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_3();
                break;
                
                case 4:
                try {
                    input_stream.backup(0);
                    while (curChar <= 32 && (0x100000200L & (1L << curChar)) != 0L) {
                        curChar = input_stream.BeginToken();
                    }
                }
                catch (java.io.IOException e1) {
                    continue EOFLoop;
                }
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_4();
                break;
                
                case 5:
                try {
                    input_stream.backup(0);
                    while (curChar <= 32 && (0x100002200L & (1L << curChar)) != 0L) {
                        curChar = input_stream.BeginToken();
                    }
                }
                catch (java.io.IOException e1) {
                    continue EOFLoop;
                }
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_5();
                break;
                
                case 6:
                try {
                    input_stream.backup(0);
                    while (curChar <= 32 && (0x100002200L & (1L << curChar)) != 0L) {
                        curChar = input_stream.BeginToken();
                    }
                }
                catch (java.io.IOException e1) {
                    continue EOFLoop;
                }
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_6();
                break;
                
                case 7:
                    jjmatchedKind = 0x7fffffff;
                    jjmatchedPos = 0;
                    curPos = jjMoveStringLiteralDfa0_7();
                    break;
                
                case 8:
                    jjmatchedKind = 0x7fffffff;
                    jjmatchedPos = 0;
                    curPos = jjMoveStringLiteralDfa0_8();
                    break;
                
                case 9:
                    jjmatchedKind = 0x7fffffff;
                    jjmatchedPos = 0;
                    curPos = jjMoveStringLiteralDfa0_9();
                    break;
            }
            if (jjmatchedKind != 0x7fffffff) {
                if (jjmatchedPos + 1 < curPos) {
                    input_stream.backup(curPos - jjmatchedPos - 1);
                }
                if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L) {
                    matchedToken = jjFillToken();
                    if (jjnewLexState[jjmatchedKind] != -1) {
                        curLexState = jjnewLexState[jjmatchedKind];
                    }
                    return matchedToken;
                }
                else {
                    if (jjnewLexState[jjmatchedKind] != -1) {
                        curLexState = jjnewLexState[jjmatchedKind];
                    }
                    continue EOFLoop;
                }
            }
            int error_line = input_stream.getEndLine();
            int error_column = input_stream.getEndColumn();
            String error_after = null;
            boolean EOFSeen = false;
            try {
                input_stream.readChar();
                input_stream.backup(1);
            }
            catch (java.io.IOException e1) {
                EOFSeen = true;
                error_after = curPos <= 1 ? "" : input_stream.GetImage();
                if (curChar == '\n' || curChar == '\r') {
                    error_line++;
                    error_column = 0;
                }
                else {
                    error_column++;
                }
            }
            if (!EOFSeen) {
                input_stream.backup(1);
                error_after = curPos <= 1 ? "" : input_stream.GetImage();
            }
            throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
        }
    }

    private void jjCheckNAdd(int state) {
        if (jjrounds[state] != jjround) {
            jjstateSet[jjnewStateCnt++] = state;
            jjrounds[state] = jjround;
        }
    }

    private void jjAddStates(int start, int end) {
        do {
            jjstateSet[jjnewStateCnt++] = jjnextStates[start];
        }
        while (start++ != end);
    }

    private void jjCheckNAddTwoStates(int state1, int state2) {
        jjCheckNAdd(state1);
        jjCheckNAdd(state2);
    }

    private void jjCheckNAddStates(int start, int end) {
        do {
            jjCheckNAdd(jjnextStates[start]);
        }
        while (start++ != end);
    }

    /**
     * Constructor.
     */
    public OneCTJTokenManager(SimpleCharStream stream) {

        if (SimpleCharStream.staticFlag) {
            throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
        }

        input_stream = stream;
    }

    /**
     * Constructor.
     */
    public OneCTJTokenManager(SimpleCharStream stream, int lexState) {
        ReInit(stream);
        SwitchTo(lexState);
    }

    /**
     * Reinitialise parser.
     */
    public void ReInit(SimpleCharStream stream) {

        jjmatchedPos = jjnewStateCnt = 0;
        curLexState = defaultLexState;
        input_stream = stream;
        ReInitRounds();
    }

    private void ReInitRounds() {
        int i;
        jjround = 0x80000001;
        for (i = 16; i-- > 0;) {
            jjrounds[i] = 0x80000000;
        }
    }

    /**
     * Reinitialise parser.
     */
    public void ReInit(SimpleCharStream stream, int lexState) {

        ReInit(stream);
        SwitchTo(lexState);
    }

    /**
     * Switch to specified lex state.
     */
    public void SwitchTo(int lexState) {
        if (lexState >= 10 || lexState < 0) {
            throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
        }
        else {
            curLexState = lexState;
        }
    }

    /**
     * Lexer state names.
     */
    public static final String[] lexStateNames = {
        "DEFAULT",
        "EVENT_DATA",
        "VALUE",
        "LOCKS",
        "LOCKS_PROPERTIES",
        "LOCKS_SINGLE_QUOTED_PROPERTIES",
        "LOCKS_DOUBLE_QUOTED_PROPERTIES",
        "LOCKS_PROPERTIES_VALUES",
        "LOCKS_SINGLE_QUOTED_PROPERTIES_VALUES",
        "LOCKS_DOUBLE_QUOTED_PROPERTIES_VALUES",};

    /**
     * Lex State array.
     */
    public static final int[] jjnewLexState = {
        -1, -1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, -1, 3, -1, -1, -1, 5, 6,
        4, -1, -1, -1, -1, 7, 1, 0, 4, 4, -1, -1, -1, -1, 8, -1, 1, 5, 5, -1, -1, -1, -1, 9, -1,
        1, 6, 6, -1, -1, -1, -1, -1, -1, -1, 0, -1, 2, -1, 0, 1, 1, 1, 1, -1,};
    static final long[] jjtoToken = {
        0xff9ffffffffffff1L, 0x3fL,};
    static final long[] jjtoSkip = {
        0xeL, 0x0L,};
    protected SimpleCharStream input_stream;

    private final int[] jjrounds = new int[16];
    private final int[] jjstateSet = new int[2 * 16];

    protected int curChar;
}
