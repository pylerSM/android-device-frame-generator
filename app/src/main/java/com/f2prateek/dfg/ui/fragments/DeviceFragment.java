/*
 * Copyright 2014 Prateek Srivastava (@f2prateek)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.f2prateek.dfg.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.OnClick;
import com.f2prateek.dart.InjectExtra;
import com.f2prateek.dfg.DeviceProvider;
import com.f2prateek.dfg.Events;
import com.f2prateek.dfg.R;
import com.f2prateek.dfg.core.AbstractGenerateFrameService;
import com.f2prateek.dfg.core.GenerateFrameService;
import com.f2prateek.dfg.model.Device;
import com.f2prateek.dfg.util.BitmapUtils;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import java.util.List;
import javax.inject.Inject;

public class DeviceFragment extends BaseFragment {

  private static final String EXTRA_DEVICE = "device";
  private static final int RESULT_SELECT_PICTURE = 542;

  @Inject Picasso picasso;
  @Inject DeviceProvider deviceProvider;

  @InjectView(R.id.tv_device_resolution) TextView deviceResolutionText;
  @InjectView(R.id.tv_device_size) TextView deviceSizeText;
  @InjectView(R.id.tv_device_name) TextView deviceNameText;
  @InjectView(R.id.iv_device_thumbnail) ImageView deviceThumbnailText;
  @InjectView(R.id.iv_device_default) ImageView deviceDefaultText;

  @InjectExtra(EXTRA_DEVICE) Device device;

  public static DeviceFragment newInstance(Device device) {
    DeviceFragment f = new DeviceFragment();
    Bundle args = new Bundle();
    args.putParcelable(EXTRA_DEVICE, device);
    f.setArguments(args);
    f.setRetainInstance(true);
    return f;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_device, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    picasso.load(BitmapUtils.getResourceIdentifierForDrawable(getActivity(),
        device.getThumbnailResourceName())).fit().centerInside().into(deviceThumbnailText);
    deviceDefaultText.bringToFront();
    deviceDefaultText.setImageResource(
        isDefault() ? R.drawable.ic_action_star_selected : R.drawable.ic_action_star);
    deviceSizeText.setText(device.physicalSize() + "\" @ " + device.density());
    deviceNameText.setText(device.name());
    deviceResolutionText.setText(device.realSize().x() + "x" + device.realSize().y());
  }

  @OnClick(R.id.iv_device_default)
  public void updateDefaultDevice() {
    if (isDefault()) {
      return;
    }
    deviceProvider.saveDefaultDevice(device);
    bus.post(new Events.DefaultDeviceUpdated(device));
  }

  @Subscribe
  public void onDefaultDeviceUpdated(Events.DefaultDeviceUpdated event) {
    deviceDefaultText.post(new Runnable() {
      @Override public void run() {
        deviceDefaultText.setImageResource(
            isDefault() ? R.drawable.ic_action_star_selected : R.drawable.ic_action_star);
      }
    });
  }

  private boolean isDefault() {
    return deviceProvider.getDefaultDevice().equals(device);
  }

  @OnClick(R.id.iv_device_thumbnail)
  public void getScreenshotImageFromUser() {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    if (isAvailable(activityContext, intent)) {
      startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)),
          RESULT_SELECT_PICTURE);
    } else {
      Crouton.makeText(getActivity(), R.string.no_apps_available, Style.ALERT).show();
    }
  }

  /**
   * Check if any apps are installed on the app to receive this intent.
   */
  public static boolean isAvailable(Context ctx, Intent intent) {
    final PackageManager mgr = ctx.getPackageManager();
    List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return list.size() > 0;
  }

  @OnClick(R.id.tv_device_name)
  public void openDevicePage() {
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setData(Uri.parse(device.url()));
    startActivity(i);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == RESULT_SELECT_PICTURE && resultCode == Activity.RESULT_OK) {
      if (data == null) {
        return;
      }
      Uri selectedImageUri = data.getData();
      Intent intent = new Intent(getActivity(), GenerateFrameService.class);
      intent.putExtra(AbstractGenerateFrameService.KEY_EXTRA_DEVICE, device);
      intent.putExtra(GenerateFrameService.KEY_EXTRA_SCREENSHOT, selectedImageUri);
      getActivity().startService(intent);
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}