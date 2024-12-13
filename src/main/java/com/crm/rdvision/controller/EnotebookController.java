package com.crm.rdvision.controller;

import com.crm.rdvision.entity.EnoteBook;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.EnoteBookRepo;
import com.crm.rdvision.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/enote")
public class EnotebookController {

    @Autowired
    EnoteBookRepo enoteBookRepo;
    @Autowired
    UserRepo userRepo;

    @PostMapping("/createNote")
    public ResponseEntity<EnoteBook> createNewNote(@RequestBody EnoteBook enoteBook){
        enoteBook.setDate(LocalDateTime.now());
        return new ResponseEntity<>(enoteBookRepo.save(enoteBook), HttpStatus.CREATED);
    }

    @GetMapping("/getallByUser/{userId}")
    public List<EnoteBook> getAllNotBookOfSpecificUser(@PathVariable int userId){
        Optional<User> user =userRepo.findByUserId(userId);
        return enoteBookRepo.findByUser(user.get());
    }

    @GetMapping("/updqateNote")
    public ResponseEntity<EnoteBook> updateEnotebook(@RequestBody EnoteBook enoteBook){
        EnoteBook enoteBook1 =enoteBookRepo.findByNoteId(enoteBook.getNoteId());
        enoteBook1.setTitle(enoteBook.getTitle());
        enoteBook1.setNoteContent(enoteBook.getNoteContent());
        return new ResponseEntity<>(enoteBookRepo.save(enoteBook1),HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/delete/{noteId}")
    public String deleteByNoteId(@PathVariable long noteId){
        EnoteBook enoteBook =enoteBookRepo.findByNoteId(noteId);
        if(enoteBook!=null){
            enoteBookRepo.deleteById(noteId);
            return "Deleted";
        }else{
            return "Note Not Found";
        }
    }
}
