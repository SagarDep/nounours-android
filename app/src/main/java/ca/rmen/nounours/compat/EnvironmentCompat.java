/*
 *   Copyright (c) 2009 - 2015 Carmen Alvarez
 *
 *   This file is part of Nounours for Android.
 *
 *   Nounours for Android is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nounours for Android is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nounours for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.nounours.compat;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import ca.rmen.nounours.util.FileUtil;
import ca.rmen.nounours.util.Trace;

public class EnvironmentCompat {

    private EnvironmentCompat() {
        // Prevent instantiation
    }
    public static File getExternalFilesDir(Context context) {
        File result;
        String folderName = "nounours";
        if (ApiHelper.getAPILevel() >= 8) {
            result = Api8Helper.getExternalFilesDir(context, folderName);
        } else {
            File sdcard = Environment.getExternalStorageDirectory();
            result = new File(sdcard, folderName);
        }
        if (!result.exists()) {
            if(!result.mkdirs() || !result.isDirectory()) {
                Trace.debug(FileUtil.class, "Could not create folder " + result);
            }
        }
        return result;
    }
}