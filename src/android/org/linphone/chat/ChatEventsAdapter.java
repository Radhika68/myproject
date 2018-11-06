/*
ChatEventsAdapter.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package org.linphone.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.linphone.LinphoneUtils;
import org.linphone.R;
import org.linphone.contacts.ContactsManager;
import org.linphone.contacts.LinphoneContact;
import org.linphone.core.Address;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatMessageListenerStub;
import org.linphone.core.Content;
import org.linphone.core.EventLog;
import org.linphone.ui.SelectableAdapter;
import org.linphone.ui.SelectableHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatEventsAdapter extends SelectableAdapter<ChatBubbleViewHolder> {
    private Context mContext;
    private List<EventLog> mHistory;
    private List<LinphoneContact> mParticipants;
    private int mItemResource;
    private GroupChatFragment mFragment;

    private ChatBubbleViewHolder.ClickListener mClickListener;

    public ChatEventsAdapter(GroupChatFragment fragment, SelectableHelper helper, int itemResource, EventLog[] history, ArrayList<LinphoneContact> participants, ChatBubbleViewHolder.ClickListener clickListener) {
        super(helper);
        mFragment = fragment;
        mContext = mFragment.getActivity();
        mItemResource = itemResource;
        mHistory = new ArrayList<>(Arrays.asList(history));
        Collections.reverse(mHistory);
        mParticipants = participants;
        mClickListener = clickListener;
    }

    @Override
    public ChatBubbleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(mItemResource, parent, false);
        ChatBubbleViewHolder VH = new ChatBubbleViewHolder(mContext, v, mClickListener);

        //Allows onLongClick ContextMenu on bubbles
        mFragment.registerForContextMenu(v);
        v.setTag(VH);
        return VH;
    }

    @Override
    public void onBindViewHolder(@NonNull ChatBubbleViewHolder holder, final int position) {
        final EventLog event = mHistory.get(position);

        holder.delete.setVisibility(View.GONE);
        holder.eventLayout.setVisibility(View.GONE);
        holder.securityEventLayout.setVisibility(View.GONE);
        holder.rightAnchor.setVisibility(View.GONE);
        holder.bubbleLayout.setVisibility(View.GONE);

        if (isEditionEnabled()) {
            holder.delete.setOnCheckedChangeListener(null);
            holder.delete.setChecked(isSelected(position));
            holder.delete.setTag(position);
            holder.delete.setVisibility(View.VISIBLE);
        }

        if (event.getType() == EventLog.Type.ConferenceChatMessage) {
            final ChatMessage message = event.getChatMessage();
            message.setUserData(holder);

            LinphoneContact contact = null;
            Address remoteSender = message.getFromAddress();
            if (!message.isOutgoing()) {
                for (LinphoneContact c : mParticipants) {
                    if (c != null && c.hasAddress(remoteSender.asStringUriOnly())) {
                        contact = c;
                        break;
                    }
                }
            }
            holder.bindMessage(message, contact);
            changeBackgroundDependingOnPreviousAndNextEvents(message, holder, position);

            message.setListener(new ChatMessageListenerStub() {
                @Override
                public void onFileTransferProgressIndication(ChatMessage message, Content content, int offset, int total) {
                    ChatBubbleViewHolder holder = (ChatBubbleViewHolder) message.getUserData();
                    if (holder == null) return;
                                /*if (offset == total) {
                                    fileTransferProgressBar.setVisibility(View.GONE);
                                    fileTransferAction.setVisibility(View.GONE);
                                    fileTransferLayout.setVisibility(View.GONE);

                                    displayAttachedFile(message, ChatBubbleViewHolder.this);
                                } else {
                                    fileTransferProgressBar.setVisibility(View.VISIBLE);
                                    fileTransferProgressBar.setProgress(offset * 100 / total);
                                }*/
                }

                @Override
                public void onMsgStateChanged(ChatMessage message, ChatMessage.State state) {
                    ChatBubbleViewHolder holder = (ChatBubbleViewHolder) message.getUserData();
                    if (holder != null) {
                        holder.bindMessage(message, null);
                        changeBackgroundDependingOnPreviousAndNextEvents(message, holder, position);
                    }
                }
            });
        } else { // Event is not chat message
            Address address = event.getParticipantAddress();
            String displayName = null;
            if (address != null) {
                LinphoneContact contact = ContactsManager.getInstance().findContactFromAddress(address);
                if (contact != null) {
                    displayName = contact.getFullName();
                } else {
                    displayName = LinphoneUtils.getAddressDisplayName(address);
                }
            }

            switch (event.getType()) {
                case ConferenceCreated:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.conference_created));
                    break;
                case ConferenceTerminated:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.conference_destroyed));
                    break;
                case ConferenceParticipantAdded:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.participant_added).replace("%s", displayName));
                    break;
                case ConferenceParticipantRemoved:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.participant_removed).replace("%s", displayName));
                    break;
                case ConferenceSubjectChanged:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.subject_changed).replace("%s", event.getSubject()));
                    break;
                case ConferenceParticipantSetAdmin:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.admin_set).replace("%s", displayName));
                    break;
                case ConferenceParticipantUnsetAdmin:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.admin_unset).replace("%s", displayName));
                    break;
                case ConferenceParticipantDeviceAdded:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.device_added).replace("%s", displayName));
                    break;
                case ConferenceParticipantDeviceRemoved:
                    holder.eventLayout.setVisibility(View.VISIBLE);
                    holder.eventMessage.setText(mContext.getString(R.string.device_removed).replace("%s", displayName));
                    break;
                case ConferenceSecurityEvent:
                    holder.securityEventLayout.setVisibility(View.VISIBLE);
                    holder.securityEventMessage.setText("TODO");
                case None:
                default:
                    //TODO
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return mHistory.size();
    }

    public void addToHistory(EventLog log) {
        mHistory.add(0, log);
        notifyItemInserted(0);
        notifyItemChanged(1);
    }

    public void addAllToHistory(ArrayList<EventLog> logs) {
        int currentSize = mHistory.size() - 1;
        Collections.reverse(logs);
        mHistory.addAll(logs);
        notifyItemRangeInserted(currentSize + 1, logs.size());
    }

    public void setContacts(ArrayList<LinphoneContact> participants) {
        mParticipants = participants;
    }

    public void refresh(EventLog[] history) {
        mHistory = new ArrayList<>(Arrays.asList(history));
        Collections.reverse(mHistory);
        notifyDataSetChanged();
    }

    public void clear() {
        for (EventLog event : mHistory) {
            if (event.getType() == EventLog.Type.ConferenceChatMessage) {
                ChatMessage message = event.getChatMessage();
                message.setListener(null);
            }
        }
        mHistory.clear();
    }

    public int getCount() {
        return mHistory.size();
    }

    public Object getItem(int i) {
        return mHistory.get(i);
    }

    public void removeItem(int i) {
        mHistory.remove(i);
        notifyItemRemoved(i);
    }

    private void changeBackgroundDependingOnPreviousAndNextEvents(ChatMessage message, ChatBubbleViewHolder holder, int position) {
        boolean hasPrevious = false, hasNext = false;

        // Do not forget history is reversed, so previous in order is next in list display !
        if (position > 0 && mContext.getResources().getBoolean(R.bool.lower_space_between_chat_bubbles_if_same_person)) {
            EventLog previousEvent = (EventLog) getItem(position - 1);
            if (previousEvent.getType() == EventLog.Type.ConferenceChatMessage) {
                ChatMessage previousMessage = previousEvent.getChatMessage();
                if (previousMessage.getFromAddress().weakEqual(message.getFromAddress())) {
                    hasPrevious = true;
                }
            }
        }
        if (position < mHistory.size() - 1 && mContext.getResources().getBoolean(R.bool.lower_space_between_chat_bubbles_if_same_person)) {
            EventLog nextEvent = (EventLog) getItem(position + 1);
            if (nextEvent.getType() == EventLog.Type.ConferenceChatMessage) {
                ChatMessage nextMessage = nextEvent.getChatMessage();
                if (nextMessage.getFromAddress().weakEqual(message.getFromAddress())) {
                    holder.timeText.setVisibility(View.GONE);
                    if (!message.isOutgoing()) {
                        holder.avatarLayout.setVisibility(View.INVISIBLE);
                    }
                    hasNext = true;
                }
            }
        }

        if (message.isOutgoing()) {
            if (hasNext && hasPrevious) {
                holder.background.setBackgroundResource(R.drawable.chat_bubble_outgoing_split_2);
            } else if (hasNext) {
                holder.background.setBackgroundResource(R.drawable.chat_bubble_outgoing_split_3);
            } else if (hasPrevious) {
                holder.background.setBackgroundResource(R.drawable.chat_bubble_outgoing_split_1);
            } else {
                holder.background.setBackgroundResource(R.drawable.chat_bubble_outgoing_full);
            }
        } else {
            if (hasNext && hasPrevious) {
                holder.background.setBackgroundResource(R.drawable.chat_bubble_incoming_split_2);
            } else if (hasNext) {
                holder.background.setBackgroundResource(R.drawable.chat_bubble_incoming_split_3);
            } else if (hasPrevious) {
                holder.background.setBackgroundResource(R.drawable.chat_bubble_incoming_split_1);
            } else {
                holder.background.setBackgroundResource(R.drawable.chat_bubble_incoming_full);
            }
        }
    }
}
