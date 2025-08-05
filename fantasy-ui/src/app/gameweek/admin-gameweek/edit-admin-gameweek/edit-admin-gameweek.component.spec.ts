import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditAdminGameweekComponent } from './edit-admin-gameweek.component';

describe('EditAdminGameweekComponent', () => {
  let component: EditAdminGameweekComponent;
  let fixture: ComponentFixture<EditAdminGameweekComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditAdminGameweekComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditAdminGameweekComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
